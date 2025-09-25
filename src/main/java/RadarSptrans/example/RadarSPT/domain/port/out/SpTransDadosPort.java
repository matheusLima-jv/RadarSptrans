package RadarSptrans.example.RadarSPT.domain.port.out;

import RadarSptrans.example.RadarSPT.domain.model.LinhaResponse;
import RadarSptrans.example.RadarSPT.domain.model.PosicaoBusResponse;

import java.util.List;

public interface SpTransDadosPort {
    List<LinhaResponse> buscarLinha(String termosBusca, String sessionCookie);

    PosicaoBusResponse buscarPosicaoLinha(String codigoLinha, String sessionCookie);
}
