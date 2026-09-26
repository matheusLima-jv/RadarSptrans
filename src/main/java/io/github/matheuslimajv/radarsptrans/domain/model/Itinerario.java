package io.github.matheuslimajv.radarsptrans.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Itinerário programado de uma linha em um sentido, com as paradas em ordem de passagem.
 */
public record Itinerario(String letreiro, int sentido, List<ParadaItinerario> paradas,
                         Set<DayOfWeek> diasOperacao, List<JanelaFrequencia> frequencias) {

    public Optional<Integer> intervaloProgramadoSegundos(LocalDateTime momento) {
        if (!diasOperacao.contains(momento.getDayOfWeek())) {
            return Optional.empty();
        }
        int segundosDoDia = momento.toLocalTime().toSecondOfDay();
        return frequencias.stream()
                .filter(janela -> janela.contem(segundosDoDia))
                .map(JanelaFrequencia::intervaloSegundos)
                .findFirst();
    }
}
