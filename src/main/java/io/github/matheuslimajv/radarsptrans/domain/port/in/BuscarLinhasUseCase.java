package io.github.matheuslimajv.radarsptrans.domain.port.in;

import io.github.matheuslimajv.radarsptrans.domain.model.Linha;

import java.util.List;

public interface BuscarLinhasUseCase {
    List<Linha> buscarLinhas(String termosBusca);
}
