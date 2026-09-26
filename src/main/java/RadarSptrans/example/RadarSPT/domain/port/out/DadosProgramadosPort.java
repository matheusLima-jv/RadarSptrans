package RadarSptrans.example.RadarSPT.domain.port.out;

import RadarSptrans.example.RadarSPT.domain.model.Itinerario;
import RadarSptrans.example.RadarSPT.domain.model.ParadaProxima;

import java.util.List;
import java.util.Optional;

/** Dados programados da SPTrans (GTFS): itinerários completos, paradas e frequências. */
public interface DadosProgramadosPort {
    boolean disponivel();

    Optional<Itinerario> buscarItinerario(String letreiro, int sentido);

    List<ParadaProxima> buscarParadasProximas(double latitude, double longitude, int raioMetros, int limite);
}
