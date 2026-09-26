package io.github.matheuslimajv.radarsptrans.domain.port.in;

import io.github.matheuslimajv.radarsptrans.domain.model.Itinerario;

public interface BuscarItinerarioUseCase {
    Itinerario buscarItinerario(String letreiro, int sentido);
}
