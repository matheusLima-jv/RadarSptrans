package RadarSptrans.example.RadarSPT.infrastructure.adapter.out.gtfs;

import RadarSptrans.example.RadarSPT.domain.model.Itinerario;
import RadarSptrans.example.RadarSPT.domain.model.Parada;
import RadarSptrans.example.RadarSPT.domain.model.ParadaItinerario;
import RadarSptrans.example.RadarSPT.domain.model.ParadaProxima;
import RadarSptrans.example.RadarSPT.domain.service.Distancia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/** Índices imutáveis de um GTFS carregado. */
final class GtfsDados {

    private final Map<Long, Parada> paradas;
    private final Map<String, Itinerario> itinerarios;
    private final Map<Long, List<String>> linhasPorParada;

    GtfsDados(Map<Long, Parada> paradas, Map<String, Itinerario> itinerarios) {
        this.paradas = Map.copyOf(paradas);
        this.itinerarios = Map.copyOf(itinerarios);
        this.linhasPorParada = indexarLinhas(itinerarios);
    }

    static String chave(String letreiro, int sentido) {
        return letreiro.toUpperCase() + "#" + sentido;
    }

    Optional<Itinerario> itinerario(String letreiro, int sentido) {
        return Optional.ofNullable(itinerarios.get(chave(letreiro, sentido)));
    }

    // Busca linear: ~22 mil paradas levam menos de 1 ms, sem justificar um índice espacial.
    List<ParadaProxima> paradasProximas(double latitude, double longitude, int raioMetros, int limite) {
        List<ParadaProxima> resultado = new ArrayList<>();
        for (Parada parada : paradas.values()) {
            double distancia = Distancia.metros(latitude, longitude, parada.latitude(), parada.longitude());
            if (distancia <= raioMetros) {
                resultado.add(new ParadaProxima(parada, (int) Math.round(distancia),
                        linhasPorParada.getOrDefault(parada.codigo(), List.of())));
            }
        }
        return resultado.stream()
                .sorted(Comparator.comparingInt(ParadaProxima::distanciaMetros))
                .limit(limite)
                .toList();
    }

    int totalParadas() {
        return paradas.size();
    }

    int totalItinerarios() {
        return itinerarios.size();
    }

    private static Map<Long, List<String>> indexarLinhas(Map<String, Itinerario> itinerarios) {
        Map<Long, TreeSet<String>> linhas = new HashMap<>();
        for (Itinerario itinerario : itinerarios.values()) {
            for (ParadaItinerario paradaItinerario : itinerario.paradas()) {
                linhas.computeIfAbsent(paradaItinerario.parada().codigo(), k -> new TreeSet<>())
                        .add(itinerario.letreiro());
            }
        }
        Map<Long, List<String>> resultado = new HashMap<>();
        linhas.forEach((parada, letreiros) -> resultado.put(parada, List.copyOf(letreiros)));
        return Map.copyOf(resultado);
    }
}
