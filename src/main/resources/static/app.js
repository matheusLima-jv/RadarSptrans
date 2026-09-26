// Radar SPTrans: busca a linha, escolhe o destino e rastreia o ônibus que vai passar na parada
// mais próxima do usuário.

// Servido pelo próprio Spring, o caminho relativo basta; para outra origem, defina window.RADAR_API_URL.
const API = window.RADAR_API_URL || '/api/sptrans';
const ATUALIZACAO_MS = 15000;
const POSICAO_ANTIGA_MS = 2 * 60 * 1000;
const MOVIMENTO_MINIMO_M = 15;
const SAO_PAULO = [-23.5505, -46.6333];

const ICONE_ONIBUS = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 16c0 .88.39 1.67 1 2.22V20a1 1 0 0 0 '
    + '1 1h1a1 1 0 0 0 1-1v-1h8v1a1 1 0 0 0 1 1h1a1 1 0 0 0 1-1v-1.78c.61-.55 1-1.34 1-2.22V6c0-3.5-3.58-4-8-4s-8 .5-8 '
    + '4v10zm3.5 1a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zm9 0a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3zM18 11H6V6h12v5z"/></svg>';

// ---------- estado ----------

const estado = {
    linha: null,               // linha escolhida (com destino)
    itinerario: null,          // paradas da linha em ordem (GTFS), ou null se indisponível
    local: null,               // { latitude, longitude } do usuário
    veiculos: [],
    tempoEspera: null,         // resultado de /paradas/tempo-espera para a linha
    rastreadoManual: null,     // { prefixo, daLista } escolhido pelo usuário; null = automático
    seguir: true,              // mapa acompanha o ônibus rastreado
    direcoes: new Map(),       // prefixo -> { latitude, longitude, rumo }
    timer: null,
    proximaAtualizacaoEm: 0,
    buscaAtual: 0
};

// ---------- utilitários ----------

// Cria elementos sem innerHTML: todo texto vindo da API entra como texto, nunca como HTML.
function el(tag, props = {}, ...filhos) {
    const elemento = document.createElement(tag);
    for (const [chave, valor] of Object.entries(props)) {
        if (valor === null || valor === undefined || valor === false) {
            continue;
        }
        if (chave === 'class') {
            elemento.className = valor;
        } else if (chave.startsWith('on')) {
            elemento.addEventListener(chave.slice(2), valor);
        } else {
            elemento.setAttribute(chave, valor === true ? '' : valor);
        }
    }
    for (const filho of filhos.flat()) {
        if (filho !== null && filho !== undefined && filho !== false) {
            elemento.append(filho instanceof Node ? filho : String(filho));
        }
    }
    return elemento;
}

const $ = id => document.getElementById(id);

async function api(caminho, parametros) {
    const resposta = await fetch(`${API}${caminho}?${new URLSearchParams(parametros)}`);
    const corpo = await resposta.json().catch(() => null);
    if (!resposta.ok) {
        const erro = new Error(corpo?.message || `Erro HTTP ${resposta.status}`);
        erro.status = resposta.status;
        throw erro;
    }
    return corpo;
}

let timerAviso = null;

function avisar(mensagem) {
    const aviso = $('aviso');
    aviso.textContent = mensagem;
    aviso.hidden = false;
    clearTimeout(timerAviso);
    timerAviso = setTimeout(() => { aviso.hidden = true; }, 5000);
}

function distanciaMetros(lat1, lon1, lat2, lon2) {
    const rad = Math.PI / 180;
    const dLat = (lat2 - lat1) * rad;
    const dLon = (lon2 - lon1) * rad;
    const a = Math.sin(dLat / 2) ** 2 + Math.cos(lat1 * rad) * Math.cos(lat2 * rad) * Math.sin(dLon / 2) ** 2;
    return 2 * 6371000 * Math.asin(Math.sqrt(a));
}

function rumoGraus(de, para) {
    const rad = Math.PI / 180;
    const y = Math.sin((para.longitude - de.longitude) * rad) * Math.cos(para.latitude * rad);
    const x = Math.cos(de.latitude * rad) * Math.sin(para.latitude * rad)
        - Math.sin(de.latitude * rad) * Math.cos(para.latitude * rad) * Math.cos((para.longitude - de.longitude) * rad);
    return (Math.atan2(y, x) / rad + 360) % 360;
}

function formatarDistancia(metros) {
    return metros < 1000 ? `${Math.round(metros)} m` : `${(metros / 1000).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} km`;
}

function formatarIdade(ms) {
    const segundos = Math.max(0, Math.round(ms / 1000));
    return segundos < 60 ? `${segundos} s` : `${Math.round(segundos / 60)} min`;
}

function formatarMinutos(valor) {
    return `${valor.toLocaleString('pt-BR', { maximumFractionDigits: 1 })} min`;
}

// ---------- mapa ----------

const mapa = L.map('mapa', { zoomControl: true }).setView(SAO_PAULO, 12);
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
}).addTo(mapa);

const camadaRota = L.layerGroup().addTo(mapa);
const camadaParadaAlvo = L.layerGroup().addTo(mapa);
const camadaOnibus = L.layerGroup().addTo(mapa);
let marcadorUsuario = null;

function iconeOnibus(veiculo, rastreado) {
    const idade = Date.now() - Date.parse(veiculo.atualizadoEm);
    const classes = ['marcador-onibus', rastreado && 'rastreado', idade > POSICAO_ANTIGA_MS && 'antigo']
        .filter(Boolean).join(' ');
    const rumo = estado.direcoes.get(veiculo.prefixo)?.rumo;
    const seta = rumo === undefined ? '' : `<span class="direcao" style="transform: rotate(${Math.round(rumo)}deg)"></span>`;
    // O prefixo é só dígitos na SPTrans, mas é escapado mesmo assim por vir da API.
    const rotulo = rastreado ? `<span class="rotulo-onibus">${escaparHtml(veiculo.prefixo)}</span>` : '';
    const tamanho = rastreado ? 40 : 30;
    return L.divIcon({
        className: '',
        html: `<div class="${classes}">${seta}${ICONE_ONIBUS}${rotulo}</div>`,
        iconSize: [tamanho, tamanho],
        iconAnchor: [tamanho / 2, tamanho / 2]
    });
}

function escaparHtml(texto) {
    return String(texto).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
}

function desenharRota() {
    camadaRota.clearLayers();
    const paradas = estado.itinerario?.paradas ?? [];
    if (paradas.length === 0) {
        return;
    }
    const pontos = paradas.map(p => [p.parada.latitude, p.parada.longitude]);
    // O traçado liga as paradas em sequência: aproxima o caminho real, que não vem do GTFS carregado.
    L.polyline(pontos, { color: '#0f766e', weight: 4, opacity: 0.55 }).addTo(camadaRota);
    paradas.forEach(({ parada }) => {
        L.circleMarker([parada.latitude, parada.longitude], {
            radius: 3, color: '#0f766e', weight: 1, fillColor: '#fff', fillOpacity: 1
        }).bindTooltip(parada.nome).addTo(camadaRota);
    });
}

function desenharUsuario() {
    if (!estado.local) {
        return;
    }
    const posicao = [estado.local.latitude, estado.local.longitude];
    if (marcadorUsuario) {
        marcadorUsuario.setLatLng(posicao);
    } else {
        marcadorUsuario = L.marker(posicao, {
            icon: L.divIcon({ className: '', html: '<div class="marcador-usuario"></div>', iconSize: [18, 18] }),
            zIndexOffset: 500
        }).bindTooltip('Você').addTo(mapa);
    }
}

function desenharParadaAlvo() {
    camadaParadaAlvo.clearLayers();
    const parada = estado.tempoEspera?.parada;
    if (!parada || !estado.local) {
        return;
    }
    L.polyline([[estado.local.latitude, estado.local.longitude], [parada.latitude, parada.longitude]], {
        color: '#2563eb', weight: 2, dashArray: '4 6'
    }).addTo(camadaParadaAlvo);
    L.circleMarker([parada.latitude, parada.longitude], {
        radius: 9, color: '#fff', weight: 3, fillColor: '#16a34a', fillOpacity: 1
    }).bindTooltip(`Sua parada: ${parada.nome}`, { permanent: true, direction: 'top', offset: [0, -8] })
        .addTo(camadaParadaAlvo);
}

function desenharOnibus(rastreado) {
    camadaOnibus.clearLayers();
    estado.veiculos.forEach(veiculo => {
        const ehRastreado = rastreado?.prefixo === veiculo.prefixo;
        L.marker([veiculo.latitude, veiculo.longitude], {
            icon: iconeOnibus(veiculo, ehRastreado),
            zIndexOffset: ehRastreado ? 1000 : 0,
            title: `Ônibus ${veiculo.prefixo}`
        }).on('click', () => rastrear(veiculo.prefixo, false)).addTo(camadaOnibus);
    });
}

// Guarda a posição anterior de cada ônibus para desenhar a seta de direção.
function atualizarDirecoes(veiculos) {
    veiculos.forEach(veiculo => {
        const anterior = estado.direcoes.get(veiculo.prefixo);
        if (!anterior) {
            estado.direcoes.set(veiculo.prefixo, { latitude: veiculo.latitude, longitude: veiculo.longitude });
            return;
        }
        if (distanciaMetros(anterior.latitude, anterior.longitude, veiculo.latitude, veiculo.longitude) >= MOVIMENTO_MINIMO_M) {
            estado.direcoes.set(veiculo.prefixo, {
                latitude: veiculo.latitude,
                longitude: veiculo.longitude,
                rumo: rumoGraus(anterior, veiculo)
            });
        }
    });
}

// ---------- etapa 1: busca e escolha do destino ----------

async function buscar(termo) {
    const busca = ++estado.buscaAtual;
    const resultados = $('resultados');
    resultados.replaceChildren(el('p', { class: 'nota' }, 'Buscando...'));
    try {
        const linhas = await api('/linhas', { termosBusca: termo });
        if (busca !== estado.buscaAtual) {
            return; // o usuário já digitou outra coisa
        }
        renderizarResultados(linhas, termo);
    } catch (erro) {
        if (busca === estado.buscaAtual) {
            resultados.replaceChildren(el('p', { class: 'nota alerta' }, `Não foi possível buscar: ${erro.message}`));
        }
    }
}

function renderizarResultados(linhas, termo) {
    const resultados = $('resultados');
    if (linhas.length === 0) {
        resultados.replaceChildren(el('p', { class: 'nota' }, `Nenhuma linha encontrada para "${termo}".`));
        return;
    }
    // Agrupa os dois sentidos de cada letreiro: o usuário escolhe pelo destino.
    const porLetreiro = new Map();
    linhas.forEach(linha => {
        if (!porLetreiro.has(linha.letreiro)) {
            porLetreiro.set(linha.letreiro, []);
        }
        porLetreiro.get(linha.letreiro).push(linha);
    });
    resultados.replaceChildren(...[...porLetreiro.entries()].map(([letreiro, sentidos]) =>
        el('div', { class: 'resultado' },
            el('div', { class: 'cabecalho' }, el('span', { class: 'letreiro' }, letreiro),
                sentidos[0].circular ? 'Linha circular' : 'Escolha o destino'),
            sentidos.sort((a, b) => a.sentido - b.sentido).map(linha =>
                el('button', { type: 'button', class: 'opcao-destino', onclick: () => escolherLinha(linha) },
                    el('div', { class: 'para' }, `Para ${linha.destino}`),
                    el('div', { class: 'de' }, `saindo de ${linha.origem}`))))));
}

async function escolherLinha(linha) {
    pararAtualizacao();
    Object.assign(estado, { linha, itinerario: null, veiculos: [], tempoEspera: null, rastreadoManual: null, seguir: true });
    estado.direcoes.clear();
    camadaOnibus.clearLayers();
    camadaParadaAlvo.clearLayers();
    camadaRota.clearLayers();

    $('letreiro').textContent = linha.letreiro;
    $('destino').textContent = linha.destino;
    $('origem').textContent = linha.origem;
    $('etapa-busca').hidden = true;
    $('etapa-rastreio').hidden = false;
    $('card-rastreio').hidden = true;
    $('card-proximos').hidden = true;
    atualizarCardLocal();

    const url = new URL(location.href);
    url.searchParams.set('linha', linha.letreiro);
    url.searchParams.set('sentido', linha.sentido);
    history.replaceState(null, '', url);

    try {
        estado.itinerario = await api('/itinerario', { letreiro: linha.letreiro, sentido: linha.sentido });
        desenharRota();
        enquadrar();
    } catch (erro) {
        // Sem itinerário o rastreio continua, só sem o traçado e a contagem de paradas.
        estado.itinerario = null;
        if (erro.status !== 404) {
            avisar(`Traçado da linha indisponível: ${erro.message}`);
        }
    }
    atualizarAgora();
}

function voltarParaBusca() {
    pararAtualizacao();
    estado.linha = null;
    camadaOnibus.clearLayers();
    camadaParadaAlvo.clearLayers();
    camadaRota.clearLayers();
    $('etapa-rastreio').hidden = true;
    $('etapa-busca').hidden = false;
    history.replaceState(null, '', location.pathname);
    $('termo').focus();
}

function enquadrar() {
    const pontos = [];
    (estado.itinerario?.paradas ?? []).forEach(p => pontos.push([p.parada.latitude, p.parada.longitude]));
    if (estado.local) {
        pontos.push([estado.local.latitude, estado.local.longitude]);
    }
    if (pontos.length > 0) {
        mapa.fitBounds(L.latLngBounds(pontos), { padding: [30, 30], maxZoom: 16 });
    }
}

// ---------- etapa 2: localização ----------

function definirLocal(latitude, longitude) {
    estado.local = { latitude, longitude };
    estado.rastreadoManual = null;
    desenharUsuario();
    atualizarCardLocal();
    if (estado.linha) {
        atualizarAgora();
    }
}

function atualizarCardLocal() {
    $('card-local').hidden = Boolean(estado.local);
}

function usarGps(silencioso) {
    if (!navigator.geolocation) {
        if (!silencioso) {
            avisar('Seu navegador não oferece localização. Toque no mapa para marcar onde você está.');
        }
        return;
    }
    navigator.geolocation.getCurrentPosition(
        posicao => {
            definirLocal(posicao.coords.latitude, posicao.coords.longitude);
            if (!estado.linha) {
                mapa.setView([posicao.coords.latitude, posicao.coords.longitude], 15);
            }
        },
        () => {
            if (!silencioso) {
                avisar('Não foi possível obter sua localização. Toque no mapa para marcar onde você está.');
            }
        },
        { enableHighAccuracy: true, timeout: 10000 });
}

// ---------- etapa 3: rastreio ----------

// A próxima atualização só é agendada quando a atual termina, e pausa com a aba oculta.
function atualizarAgora() {
    pararAtualizacao();
    atualizar().finally(agendar);
}

function agendar() {
    clearTimeout(estado.timer);
    if (estado.linha && !document.hidden) {
        estado.proximaAtualizacaoEm = Date.now() + ATUALIZACAO_MS;
        estado.timer = setTimeout(atualizarAgora, ATUALIZACAO_MS);
    }
}

function pararAtualizacao() {
    clearTimeout(estado.timer);
    estado.timer = null;
    estado.proximaAtualizacaoEm = 0;
}

async function atualizar() {
    const linha = estado.linha;
    if (!linha) {
        return;
    }
    const local = estado.local;
    const [posicao, tempoEspera] = await Promise.all([
        api('/posicao', { codigoLinha: linha.codigo }).catch(erro => {
            avisar(`Posição dos ônibus indisponível: ${erro.message}`);
            return null;
        }),
        local ? api('/paradas/tempo-espera', {
            termosBusca: linha.letreiro, codigoLinha: linha.codigo,
            latitude: local.latitude, longitude: local.longitude
        }).then(r => r[0] ?? null).catch(erro => {
            avisar(`Tempo de espera indisponível: ${erro.message}`);
            return null;
        }) : Promise.resolve(null)
    ]);
    if (linha !== estado.linha) {
        return; // trocou de linha durante a requisição
    }
    if (posicao) {
        estado.veiculos = posicao.veiculos.filter(v => Number.isFinite(v.latitude) && Number.isFinite(v.longitude));
        atualizarDirecoes(estado.veiculos);
    }
    estado.tempoEspera = tempoEspera;
    renderizar();
}

// O ônibus rastreado é o próximo a chegar na parada do usuário (ou o escolhido na lista, enquanto
// ele ainda estiver a caminho). Sem ônibus a caminho, mostra o mais perto do usuário.
function escolherRastreado() {
    const chegadas = estado.tempoEspera?.chegadas ?? [];
    const noMapa = new Map(estado.veiculos.map(v => [v.prefixo, v]));
    const manual = estado.rastreadoManual;
    if (manual) {
        const chegadaManual = chegadas.find(c => c.prefixo === manual.prefixo);
        const veiculoManual = noMapa.get(manual.prefixo) ?? null;
        // Escolhido na lista de próximos: volta ao automático quando passa da parada.
        // Clicado no mapa: segue rastreado enquanto a SPTrans informar a posição.
        if (chegadaManual || (!manual.daLista && veiculoManual)) {
            return { chegada: chegadaManual ?? null, veiculo: veiculoManual };
        }
        estado.rastreadoManual = null;
    }
    const chegada = chegadas.find(c => noMapa.has(c.prefixo)) ?? chegadas[0];
    if (chegada) {
        return { chegada, veiculo: noMapa.get(chegada.prefixo) ?? null };
    }
    if (estado.local && estado.veiculos.length > 0) {
        const { latitude, longitude } = estado.local;
        const maisPerto = estado.veiculos.reduce((a, b) =>
            distanciaMetros(latitude, longitude, a.latitude, a.longitude)
            <= distanciaMetros(latitude, longitude, b.latitude, b.longitude) ? a : b);
        return { chegada: null, veiculo: maisPerto, semChegada: true };
    }
    return null;
}

function rastrear(prefixo, daLista) {
    estado.rastreadoManual = { prefixo, daLista };
    estado.seguir = true;
    renderizar();
}

function indiceParadaMaisProxima(latitude, longitude) {
    const paradas = estado.itinerario?.paradas ?? [];
    let melhor = -1;
    let menor = Infinity;
    paradas.forEach(({ parada }, i) => {
        const d = distanciaMetros(latitude, longitude, parada.latitude, parada.longitude);
        if (d < menor) {
            menor = d;
            melhor = i;
        }
    });
    return melhor;
}

function paradasAteUsuario(veiculo) {
    const alvo = estado.tempoEspera?.parada;
    if (!veiculo || !alvo || !estado.itinerario) {
        return null;
    }
    const indiceAlvo = estado.itinerario.paradas.findIndex(p => p.parada.codigo === alvo.codigo);
    const indiceOnibus = indiceParadaMaisProxima(veiculo.latitude, veiculo.longitude);
    return indiceAlvo >= 0 && indiceOnibus >= 0 && indiceOnibus <= indiceAlvo ? indiceAlvo - indiceOnibus : null;
}

function renderizar() {
    const rastreado = escolherRastreado();
    desenharOnibus(rastreado?.veiculo);
    desenharParadaAlvo();
    renderizarCardRastreio(rastreado);
    renderizarProximos(rastreado);

    const operando = estado.veiculos.length;
    $('info-linha').textContent = `${operando} ônibus desta linha em operação agora.`;

    if (estado.seguir && rastreado?.veiculo) {
        const onibus = [rastreado.veiculo.latitude, rastreado.veiculo.longitude];
        const parada = estado.tempoEspera?.parada;
        if (rastreado.chegada && parada && estado.local) {
            // Ônibus vindo para a sua parada: enquadra ônibus, parada e você; o zoom aumenta conforme ele chega.
            mapa.fitBounds(L.latLngBounds([onibus, [parada.latitude, parada.longitude],
                [estado.local.latitude, estado.local.longitude]]), { padding: [50, 50], maxZoom: 17 });
        } else if (mapa.getZoom() < 14) {
            mapa.setView(onibus, 15);
        } else {
            mapa.panTo(onibus);
        }
    }
}

function renderizarCardRastreio(rastreado) {
    const card = $('card-rastreio');
    card.hidden = false;
    const tempoEspera = estado.tempoEspera;

    if (!estado.local) {
        card.hidden = true;
        return;
    }
    if (!rastreado) {
        card.replaceChildren(
            el('h3', {}, 'Nenhum ônibus em operação'),
            el('p', { class: 'nota' }, 'A SPTrans não informa nenhum ônibus desta linha agora.'),
            programado(tempoEspera));
        return;
    }

    const { chegada, veiculo, semChegada } = rastreado;
    const filhos = [];
    const acessivel = (chegada ?? veiculo)?.acessivel;
    filhos.push(el('h3', {}, `Ônibus ${(chegada ?? veiculo).prefixo}`, acessivel ? ' · ♿ acessível' : ''));

    if (chegada) {
        const aproximado = tempoEspera.fonteChegadas === 'POSICAO_VEICULOS';
        filhos.push(el('div', { class: 'chegada' },
            chegada.minutos === 0 ? 'chegando' : `${aproximado ? '~' : ''}${chegada.minutos} min`,
            el('small', {}, ` · previsto ${chegada.horario}`)));
        filhos.push(el('p', { class: 'nota' }, `na parada ${tempoEspera.parada.nome}, a ${formatarDistancia(tempoEspera.distanciaMetros)} de você`));
    } else if (!semChegada) {
        filhos.push(el('p', { class: 'nota' }, 'Este ônibus não está entre os próximos a passar pela sua parada.'));
    } else {
        filhos.push(el('p', {}, el('strong', {}, 'Nenhum ônibus a caminho da sua parada agora.')));
        filhos.push(el('p', { class: 'nota' }, 'Este é o ônibus da linha mais perto de você, mas ele não vai passar '
            + 'pela sua parada neste sentido (já passou ou está no fim do trajeto).'));
    }

    if (veiculo) {
        const metricas = [];
        const paradas = paradasAteUsuario(veiculo);
        if (paradas !== null && chegada) {
            metricas.push(metrica(paradas === 0 ? 'na parada' : String(paradas), paradas === 1 ? 'parada antes' : 'paradas antes'));
        }
        const referencia = chegada ? tempoEspera.parada : estado.local;
        metricas.push(metrica(formatarDistancia(distanciaMetros(veiculo.latitude, veiculo.longitude,
            referencia.latitude, referencia.longitude)), chegada ? 'da sua parada' : 'de você'));
        const idade = Date.now() - Date.parse(veiculo.atualizadoEm);
        metricas.push(metrica(formatarIdade(idade), 'desde a posição', idade > POSICAO_ANTIGA_MS));
        filhos.push(el('div', { class: 'metricas' }, metricas));
        if (idade > POSICAO_ANTIGA_MS) {
            filhos.push(el('p', { class: 'nota alerta' }, 'A posição deste ônibus está desatualizada.'));
        }
        const caixa = el('input', { type: 'checkbox', id: 'seguir', checked: estado.seguir });
        caixa.addEventListener('change', () => {
            estado.seguir = caixa.checked;
            if (estado.seguir) {
                renderizar();
            }
        });
        filhos.push(el('label', { class: 'seguir', for: 'seguir' }, caixa, 'Seguir o ônibus no mapa'));
    } else if (chegada) {
        filhos.push(el('p', { class: 'nota' }, 'A SPTrans prevê este ônibus, mas ainda não publicou a posição dele.'));
    }

    if (chegada) {
        filhos.push(el('p', { class: 'nota' }, tempoEspera.fonteChegadas === 'PREVISAO_SPTRANS'
            ? 'Fonte: previsão da SPTrans.'
            : 'Fonte: estimativa pela posição do ônibus e horário programado (não considera o trânsito).'));
    }
    filhos.push(programado(tempoEspera));
    card.replaceChildren(...filhos);
}

function metrica(valor, rotulo, alerta) {
    return el('div', { class: 'metrica' }, el('strong', { class: alerta ? 'alerta' : null }, valor), el('span', {}, rotulo));
}

function programado(tempoEspera) {
    if (!tempoEspera) {
        return el('p', { class: 'nota' }, 'Horário programado indisponível.');
    }
    return el('p', { class: 'nota' }, tempoEspera.intervaloProgramadoMinutos !== null
        ? `Programado: passa a cada ${formatarMinutos(tempoEspera.intervaloProgramadoMinutos)} `
            + `(espera média de ${formatarMinutos(tempoEspera.esperaMediaMinutos)}).`
        : 'Sem operação programada para este horário.');
}

function renderizarProximos(rastreado) {
    const card = $('card-proximos');
    const chegadas = estado.tempoEspera?.chegadas ?? [];
    if (chegadas.length < 2) {
        card.hidden = true;
        return;
    }
    const aproximado = estado.tempoEspera.fonteChegadas === 'POSICAO_VEICULOS' ? '~' : '';
    card.hidden = false;
    card.replaceChildren(
        el('h3', {}, 'Próximos ônibus na sua parada'),
        el('ul', { class: 'lista-proximos' }, chegadas.slice(0, 5).map(chegada =>
            el('li', {}, el('button', {
                type: 'button',
                'aria-pressed': String(rastreado?.chegada?.prefixo === chegada.prefixo),
                onclick: () => rastrear(chegada.prefixo, true)
            }, el('span', {}, `Ônibus ${chegada.prefixo}`), el('strong', {}, `${aproximado}${chegada.minutos} min`))))));
}

// Contagem regressiva visível até a próxima atualização.
setInterval(() => {
    const restante = estado.proximaAtualizacaoEm - Date.now();
    $('contador').textContent = estado.linha && restante > 0
        ? `Atualiza em ${Math.ceil(restante / 1000)} s`
        : (estado.linha ? 'Atualizando...' : '');
}, 1000);

// ---------- eventos ----------

let timerDigitacao = null;
$('termo').addEventListener('input', evento => {
    clearTimeout(timerDigitacao);
    const termo = evento.target.value.trim();
    if (termo.length >= 2) {
        timerDigitacao = setTimeout(() => buscar(termo), 400);
    }
});

$('form-busca').addEventListener('submit', evento => {
    evento.preventDefault();
    clearTimeout(timerDigitacao);
    const termo = $('termo').value.trim();
    if (termo) {
        buscar(termo);
    }
});

$('trocar-linha').addEventListener('click', voltarParaBusca);
$('usar-gps').addEventListener('click', () => usarGps(false));
$('atualizar').addEventListener('click', atualizarAgora);

mapa.on('click', evento => definirLocal(evento.latlng.lat, evento.latlng.lng));
// Arrastar o mapa desliga o "seguir", senão a próxima atualização puxa a tela de volta.
mapa.on('dragstart', () => {
    if (estado.seguir) {
        estado.seguir = false;
        const caixa = $('seguir');
        if (caixa) {
            caixa.checked = false;
        }
    }
});

document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
        pararAtualizacao();
    } else if (estado.linha) {
        atualizarAgora();
    }
});

// ---------- início ----------

// Link compartilhável: ?linha=848L-10&sentido=1 abre direto no rastreio.
async function restaurarDaUrl() {
    const parametros = new URLSearchParams(location.search);
    const letreiro = parametros.get('linha');
    const sentido = Number(parametros.get('sentido'));
    if (!letreiro) {
        return;
    }
    $('termo').value = letreiro;
    try {
        const linhas = await api('/linhas', { termosBusca: letreiro });
        const linha = linhas.find(l => l.letreiro.toUpperCase() === letreiro.toUpperCase() && l.sentido === sentido);
        if (linha) {
            escolherLinha(linha);
        } else {
            renderizarResultados(linhas, letreiro);
        }
    } catch (erro) {
        avisar(`Não foi possível abrir a linha ${letreiro}: ${erro.message}`);
    }
}

// Só pede o GPS sozinho se o usuário já autorizou antes; senão espera o botão.
navigator.permissions?.query({ name: 'geolocation' })
    .then(permissao => {
        if (permissao.state === 'granted') {
            usarGps(true);
        }
    })
    .catch(() => {});

restaurarDaUrl();
