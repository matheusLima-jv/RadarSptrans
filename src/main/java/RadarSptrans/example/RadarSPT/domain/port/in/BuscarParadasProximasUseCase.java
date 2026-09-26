package RadarSptrans.example.RadarSPT.domain.port.in;

import RadarSptrans.example.RadarSPT.domain.model.ParadaProxima;

import java.util.List;

public interface BuscarParadasProximasUseCase {
    List<ParadaProxima> buscarParadasProximas(double latitude, double longitude, int raioMetros, int limite);
}
