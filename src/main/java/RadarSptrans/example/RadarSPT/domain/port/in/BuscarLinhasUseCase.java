package RadarSptrans.example.RadarSPT.domain.port.in;

import RadarSptrans.example.RadarSPT.domain.model.LinhaResponse;

import java.util.List;

public interface BuscarLinhasUseCase {
    List<LinhaResponse> buscarLinhas(String termosBusca);
}
