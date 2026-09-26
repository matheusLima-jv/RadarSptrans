package io.github.matheuslimajv.radarsptrans.domain.model;

import java.util.List;

public record PrevisaoParada(String horaReferencia, List<ChegadaPrevista> chegadas) {

    public static PrevisaoParada vazia(String horaReferencia) {
        return new PrevisaoParada(horaReferencia, List.of());
    }
}
