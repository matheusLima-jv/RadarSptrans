// Cria o mapa sem localização inicial
const map = L.map('map');

// Adiciona o layer de mapa OpenStreetMap
L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
}).addTo(map);

// Função para adicionar um marcador no mapa
function addMarker(lat, lon, label) {
    if (lat && lon) {
        L.marker([lat, lon]).addTo(map)
            .bindPopup(label)
            .openPopup();
    } else {
        console.error(`Coordenadas inválidas para o marcador: ${lat}, ${lon}`);
    }
}

// Função para buscar dados da API e adicionar os veículos no mapa
async function fetchVehicleLocations() {
    const apiUrl = 'http://localhost:8080/api/sptrans/buscar?termosBusca=8055&indice=1'; // Substitua pela URL correta da sua API

    try {
        const response = await fetch(apiUrl);
        const data = await response.json();

        console.log("Dados recebidos da API:", data); // Verifica o que está sendo retornado

        // Itera sobre os veículos no array 'vs'
        data.vs.forEach(vehicle => {
            const { py: latitude, px: longitude, p: id } = vehicle;
            console.log(`Adicionando veículo ${id} em [${latitude}, ${longitude}]`);

            // Adiciona um marcador no mapa para cada veículo
            if (latitude && longitude) {
                addMarker(latitude, longitude, `Veículo ${id}`);
            }
        });
    } catch (error) {
        console.error('Erro ao buscar dados da API:', error);
    }
}

// Função para obter a localização do navegador e centralizar o mapa
function centerMapOnLocation() {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(position => {
            const userLat = position.coords.latitude;
            const userLon = position.coords.longitude;

            // Centraliza o mapa na localização do usuário
            map.setView([userLat, userLon], 12);

            // Adiciona um marcador para a localização do usuário
            addMarker(userLat, userLon, 'Você está aqui!');

            // Busca os veículos da API e adiciona no mapa
            fetchVehicleLocations();
        }, error => {
            console.error('Erro ao obter localização do navegador:', error);
            // Caso a localização falhe, define uma localização padrão
            map.setView([-23.5505, -46.6333], 12); // São Paulo como fallback
            fetchVehicleLocations();
        });
    } else {
        console.error('Geolocalização não é suportada pelo navegador.');
        // Se geolocalização não estiver disponível, usa uma localização padrão
        map.setView([-23.5505, -46.6333], 12); // São Paulo como fallback
        fetchVehicleLocations();
    }
}

// Centraliza o mapa com base na localização do navegador
centerMapOnLocation();