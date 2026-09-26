// URL base da API; pode ser sobrescrita definindo window.RADAR_API_URL antes deste script
const API_BASE_URL = window.RADAR_API_URL || 'http://localhost:8080/api/sptrans';
const INTERVALO_ATUALIZACAO_MS = 15000;
const SAO_PAULO = [-23.5505, -46.6333];

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

// Os veículos ficam num grupo próprio para serem substituídos a cada atualização
const camadaVeiculos = L.layerGroup().addTo(map);

const formBusca = document.getElementById('form-busca');
const campoTermo = document.getElementById('termo');
const listaLinhas = document.getElementById('linhas');
const status = document.getElementById('status');

let linhaSelecionada = null;
let timerAtualizacao = null;

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

// Ex.: "8000-10 · TERM. LAPA → PCA. RAMOS DE AZEVEDO" (sl = 1 é o sentido principal → secundário)
function descreverLinha(linha) {
    const sentido = linha.sl === 1 ? `${linha.tp} → ${linha.ts}` : `${linha.ts} → ${linha.tp}`;
    return `${linha.lt}-${linha.tl} · ${sentido}`;
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
            botao.textContent = descreverLinha(linha); // textContent evita injeção de HTML
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

    clearInterval(timerAtualizacao);
    atualizarVeiculos(true);
    timerAtualizacao = setInterval(() => atualizarVeiculos(false), INTERVALO_ATUALIZACAO_MS);
}

async function atualizarVeiculos(enquadrar) {
    const linha = linhaSelecionada;
    try {
        const data = await chamarApi('/posicao', { codigoLinha: linha.cl });
        if (linha !== linhaSelecionada) {
            return; // o usuário trocou de linha enquanto a requisição estava em andamento
        }

        const veiculos = (data.vs || []).filter(v => Number.isFinite(v.py) && Number.isFinite(v.px));
        camadaVeiculos.clearLayers();
        veiculos.forEach(({ py, px, p, a, ta }) => {
            const popup = document.createElement('div');
            popup.innerText = `Veículo ${p}\n${a ? 'Acessível' : 'Não acessível'}\n`
                + `Atualizado às ${new Date(ta).toLocaleTimeString('pt-BR')}`;
            L.marker([py, px], { icon: vehicleIcon }).bindPopup(popup).addTo(camadaVeiculos);
        });

        if (enquadrar && veiculos.length > 0) {
            map.fitBounds(L.latLngBounds(veiculos.map(v => [v.py, v.px])), { padding: [40, 40], maxZoom: 15 });
        }
        mostrarStatus(`${descreverLinha(linha)}: ${veiculos.length} veículo(s) em operação às ${data.hr}.`);
    } catch (error) {
        mostrarStatus(`Erro ao atualizar veículos: ${error.message}`);
    }
}

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
        map.setView([latitude, longitude], 13);
        L.marker([latitude, longitude], { icon: userIcon }).addTo(map).bindPopup('Você está aqui!');
    }, error => {
        console.error('Erro ao obter localização do navegador:', error);
    });
}

centerMapOnLocation();
