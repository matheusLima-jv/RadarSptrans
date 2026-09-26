package io.github.matheuslimajv.radarsptrans.domain.port.out;

import io.github.matheuslimajv.radarsptrans.domain.model.Itinerario;
import io.github.matheuslimajv.radarsptrans.domain.model.ParadaProxima;

import java.util.List;
import java.util.Optional;

/** Dados programados da SPTrans (GTFS): itinerários completos, paradas e frequências. */
public interface DadosProgramadosPort {
    boolean disponivel();

    Optional<Itinerario> buscarItinerario(String letreiro, int sentido);

    List<ParadaProxima> buscarParadasProximas(double latitude, double longitude, int raioMetros, int limite);
}
