// URL base da API; servido pelo próprio Spring, o caminho relativo basta.
// Para usar a API em outro endereço, defina window.RADAR_API_URL antes deste script.
const API_BASE_URL = window.RADAR_API_URL || '/api/sptrans';
const INTERVALO_ATUALIZACAO_MS = 15000;
const RAIO_PARADAS_PROXIMAS_M = 400;
const SAO_PAULO = [-23.5505, -46.6333];

const FONTES = {
    PREVISAO_SPTRANS: 'previsão SPTrans',
    POSICAO_VEICULOS: 'estimativa pela posição dos ônibus, aproximada'
};

// Cria o mapa sem localização inicial
const map = L.map('map');

// Adiciona o layer de mapa OpenStreetMap
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
}).addTo(map);

// Definindo ícones personalizados
const userIcon = L.icon({
    iconUrl: 'house.png', // Ícone de casinha
    iconSize: [32, 32], // Tamanho do ícone
    iconAnchor: [16, 32], // Posição da âncora do ícone (base)
    popupAnchor: [0, -32]  // Posição da âncora do popup em relação ao ícone
});

const vehicleIcon = L.icon({
    iconUrl: 'bus.png', // Ícone de ônibus
    iconSize: [32, 32],
    iconAnchor: [16, 32],
    popupAnchor: [0, -32]
});

// Cada camada é substituída por inteiro a cada atualização
const camadaVeiculos = L.layerGroup().addTo(map);
const camadaParadasProximas = L.layerGroup().addTo(map);
const camadaParadaAlvo = L.layerGroup().addTo(map);

const formBusca = document.getElementById('form-busca');
const campoTermo = document.getElementById('termo');
const listaLinhas = document.getElementById('linhas');
const painelEspera = document.getElementById('espera');
const status = document.getElementById('status');
const dica = document.getElementById('dica');

let linhaSelecionada = null;
let localUsuario = null;
let marcadorUsuario = null;
let timerAtualizacao = null;
let enquadrarNaProxima = false;

function mostrarStatus(mensagem) {
    status.textContent = mensagem;
}

// Faz a chamada à API e converte erros no formato ApiErrorResponse em mensagens legíveis
async function chamarApi(caminho, parametros) {
    const url = `${API_BASE_URL}${caminho}?${new URLSearchParams(parametros)}`;
    const response = await fetch(url);
    const data = await response.json().catch(() => null);
    if (!response.ok) {
        throw new Error(data?.message || `Erro HTTP ${response.status}`);
    }
    return data;
}

// Ex.: "8000-10 · TERM. LAPA → PÇA. RAMOS DE AZEVEDO"
function descreverLinha(linha) {
    return `${linha.letreiro} · ${linha.origem} → ${linha.destino}`;
}

function formatarMinutos(valor) {
    return `${valor.toLocaleString('pt-BR', { maximumFractionDigits: 1 })} min`;
}

// Monta um elemento com linhas de texto; textContent evita injeção de HTML vindo da API
function elementoTexto(linhas, classe) {
    const elemento = document.createElement('div');
    if (classe) {
        elemento.className = classe;
    }
    elemento.innerText = linhas.join('\n');
    return elemento;
}

async function buscarLinhas(termo) {
    mostrarStatus('Buscando linhas...');
    listaLinhas.replaceChildren();
    try {
        const linhas = await chamarApi('/linhas', { termosBusca: termo });
        if (linhas.length === 0) {
            mostrarStatus('Nenhuma linha encontrada.');
            return;
        }
        linhas.forEach(linha => {
            const botao = document.createElement('button');
            botao.type = 'button';
            botao.textContent = descreverLinha(linha);
            botao.addEventListener('click', () => selecionarLinha(linha, botao));
            const item = document.createElement('li');
            item.appendChild(botao);
            listaLinhas.appendChild(item);
        });
        mostrarStatus(`${linhas.length} linha(s) encontrada(s). Selecione uma.`);
    } catch (error) {
        mostrarStatus(`Erro ao buscar linhas: ${error.message}`);
    }
}

function selecionarLinha(linha, botao) {
    listaLinhas.querySelectorAll('button').forEach(b => b.classList.remove('ativa'));
    botao.classList.add('ativa');
    linhaSelecionada = linha;
    enquadrarNaProxima = true;
    camadaVeiculos.clearLayers();
    camadaParadaAlvo.clearLayers();
    painelEspera.hidden = true;
    atualizarAgora();
}

// A próxima atualização só é agendada quando a atual termina: uma resposta lenta da SPTrans
// (login + consulta podem passar de 15s) não acumula requisições.
function atualizarAgora() {
    clearTimeout(timerAtualizacao);
    timerAtualizacao = null;
    atualizar().finally(agendarAtualizacao);
}

function agendarAtualizacao() {
    clearTimeout(timerAtualizacao);
    if (linhaSelecionada && !document.hidden) {
        timerAtualizacao = setTimeout(atualizarAgora, INTERVALO_ATUALIZACAO_MS);
    }
}

// Com a aba em segundo plano não há por que continuar consultando a SPTrans
document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
        clearTimeout(timerAtualizacao);
    } else if (linhaSelecionada) {
        atualizarAgora();
    }
});

async function atualizar() {
    const linha = linhaSelecionada;
    if (!linha) {
        return;
    }
    await Promise.all([atualizarVeiculos(linha), atualizarTempoEspera(linha)]);
}

async function atualizarVeiculos(linha) {
    try {
        const posicao = await chamarApi('/posicao', { codigoLinha: linha.codigo });
        if (linha !== linhaSelecionada) {
            return; // o usuário trocou de linha enquanto a requisição estava em andamento
        }

        const veiculos = posicao.veiculos.filter(v => Number.isFinite(v.latitude) && Number.isFinite(v.longitude));
        camadaVeiculos.clearLayers();
        veiculos.forEach(({ latitude, longitude, prefixo, acessivel, atualizadoEm }) => {
            const popup = elementoTexto([
                `Veículo ${prefixo}`,
                acessivel ? 'Acessível' : 'Não acessível',
                `Atualizado às ${new Date(atualizadoEm).toLocaleTimeString('pt-BR')}`
            ]);
            L.marker([latitude, longitude], { icon: vehicleIcon }).bindPopup(popup).addTo(camadaVeiculos);
        });

        if (enquadrarNaProxima && veiculos.length > 0) {
            const pontos = veiculos.map(v => [v.latitude, v.longitude]);
            if (localUsuario) {
                pontos.push([localUsuario.latitude, localUsuario.longitude]);
            }
            // O painel cobre o lado direito do mapa em telas largas
            const larguraPainel = document.getElementById('painel').offsetWidth;
            const direita = window.innerWidth > 2 * larguraPainel ? larguraPainel + 20 : 40;
            map.fitBounds(L.latLngBounds(pontos), {
                paddingTopLeft: [40, 40], paddingBottomRight: [direita, 40], maxZoom: 15
            });
            enquadrarNaProxima = false;
        }
        mostrarStatus(`${descreverLinha(linha)}: ${veiculos.length} veículo(s) em operação às ${posicao.horaReferencia}.`);
    } catch (error) {
        mostrarStatus(`Erro ao atualizar veículos: ${error.message}`);
    }
}

async function atualizarTempoEspera(linha) {
    if (!localUsuario) {
        painelEspera.hidden = true;
        return;
    }
    try {
        const resultados = await chamarApi('/paradas/tempo-espera', {
            termosBusca: linha.letreiro,
            codigoLinha: linha.codigo,
            latitude: localUsuario.latitude,
            longitude: localUsuario.longitude
        });
        if (linha !== linhaSelecionada) {
            return;
        }
        if (resultados.length === 0) {
            mostrarEspera([`Sem itinerário programado para ${linha.letreiro}.`]);
            camadaParadaAlvo.clearLayers();
            return;
        }
        renderizarTempoEspera(resultados[0]);
    } catch (error) {
        mostrarEspera([`Tempo de espera indisponível: ${error.message}`]);
    }
}

function renderizarTempoEspera(resultado) {
    const { parada, distanciaMetros, intervaloProgramadoMinutos, esperaMediaMinutos, chegadas,
        proximaChegadaMinutos, fonteChegadas } = resultado;

    const conteudo = [elementoTexto([`Parada mais próxima: ${parada.nome} (${distanciaMetros} m)`])];

    if (proximaChegadaMinutos !== null) {
        const aproximado = fonteChegadas === 'POSICAO_VEICULOS' ? '~' : '';
        conteudo.push(elementoTexto([`Próximo ônibus em ${aproximado}${proximaChegadaMinutos} min`], 'destaque'));
        const seguintes = chegadas.slice(1, 4).map(c => `${aproximado}${c.minutos} min`);
        const detalhes = [`Fonte: ${FONTES[fonteChegadas]}`];
        if (seguintes.length > 0) {
            detalhes.unshift(`Seguintes: ${seguintes.join(', ')}`);
        }
        conteudo.push(elementoTexto(detalhes, 'nota'));
    } else {
        conteudo.push(elementoTexto(['Nenhum ônibus a caminho desta parada agora.'], 'destaque'));
    }

    conteudo.push(elementoTexto([intervaloProgramadoMinutos !== null
        ? `Passa a cada ${formatarMinutos(intervaloProgramadoMinutos)} (programado); `
            + `espera média de ${formatarMinutos(esperaMediaMinutos)}.`
        : 'Sem operação programada para este horário.'], 'nota'));

    painelEspera.replaceChildren(...conteudo);
    painelEspera.hidden = false;

    camadaParadaAlvo.clearLayers();
    L.circleMarker([parada.latitude, parada.longitude], {
        radius: 10, color: '#15803d', weight: 3, fillColor: '#22c55e', fillOpacity: 0.9
    }).bindPopup(elementoTexto([parada.nome, parada.endereco || ''].filter(Boolean)))
        .addTo(camadaParadaAlvo);
}

function mostrarEspera(linhas) {
    painelEspera.replaceChildren(elementoTexto(linhas, 'nota'));
    painelEspera.hidden = false;
}

async function carregarParadasProximas() {
    camadaParadasProximas.clearLayers();
    try {
        const paradas = await chamarApi('/paradas/proximas', {
            latitude: localUsuario.latitude,
            longitude: localUsuario.longitude,
            raio: RAIO_PARADAS_PROXIMAS_M
        });
        paradas.forEach(({ parada, distanciaMetros, linhas }) => {
            const popup = elementoTexto([
                `${parada.nome} (${distanciaMetros} m)`,
                linhas.length > 0 ? `Linhas: ${linhas.join(', ')}` : 'Sem linhas programadas'
            ]);
            L.circleMarker([parada.latitude, parada.longitude], {
                radius: 5, color: '#1d4ed8', weight: 1, fillColor: '#3b82f6', fillOpacity: 0.8
            }).bindPopup(popup).addTo(camadaParadasProximas);
        });
        dica.textContent = `${paradas.length} parada(s) a até ${RAIO_PARADAS_PROXIMAS_M} m. `
            + 'Clique no mapa para mudar sua posição.';
    } catch (error) {
        dica.textContent = `Paradas próximas indisponíveis: ${error.message}`;
    }
}

function definirLocalUsuario(latitude, longitude) {
    localUsuario = { latitude, longitude };
    if (marcadorUsuario) {
        marcadorUsuario.setLatLng([latitude, longitude]);
    } else {
        marcadorUsuario = L.marker([latitude, longitude], { icon: userIcon }).addTo(map).bindPopup('Você está aqui!');
    }
    carregarParadasProximas();
    if (linhaSelecionada) {
        atualizarAgora();
    }
}

map.on('click', event => definirLocalUsuario(event.latlng.lat, event.latlng.lng));

formBusca.addEventListener('submit', event => {
    event.preventDefault();
    const termo = campoTermo.value.trim();
    if (termo) {
        buscarLinhas(termo);
    }
});

// Centraliza o mapa na localização do navegador, com São Paulo como fallback
function centerMapOnLocation() {
    map.setView(SAO_PAULO, 12);
    if (!navigator.geolocation) {
        console.error('Geolocalização não é suportada pelo navegador.');
        return;
    }
    navigator.geolocation.getCurrentPosition(position => {
        const { latitude, longitude } = position.coords;
        map.setView([latitude, longitude], 15);
        definirLocalUsuario(latitude, longitude);
    }, error => {
        console.error('Erro ao obter localização do navegador:', error);
    });
}

centerMapOnLocation();
