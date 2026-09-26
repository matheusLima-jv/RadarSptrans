package io.github.matheuslimajv.radarsptrans.domain.port.in;

import io.github.matheuslimajv.radarsptrans.domain.model.TempoEsperaParada;

import java.util.List;

public interface CalcularTempoEsperaUseCase {
    /**
     * Para cada linha encontrada pelo termo (opcionalmente só a de código informado), localiza a parada dela
     * mais próxima do ponto e calcula o tempo de espera. Resultado ordenado pela distância até a parada.
     */
    List<TempoEsperaParada> calcularTempoEspera(String termosBusca, Integer codigoLinha,
                                                double latitude, double longitude);
}
