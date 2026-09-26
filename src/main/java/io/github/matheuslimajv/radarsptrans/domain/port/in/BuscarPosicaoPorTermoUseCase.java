package io.github.matheuslimajv.radarsptrans.domain.port.in;

import io.github.matheuslimajv.radarsptrans.domain.model.PosicaoLinha;

public interface BuscarPosicaoPorTermoUseCase {
    PosicaoLinha buscarPorTermo(String termosBusca, int indice);
}
