package io.github.matheuslimajv.radarsptrans.domain.port.in;

import io.github.matheuslimajv.radarsptrans.domain.model.ParadaProxima;

import java.util.List;

public interface BuscarParadasProximasUseCase {
    List<ParadaProxima> buscarParadasProximas(double latitude, double longitude, int raioMetros, int limite);
}
