package RadarSptrans.example.RadarSPT.domain.port.out;

import RadarSptrans.example.RadarSPT.domain.model.Linha;
import RadarSptrans.example.RadarSPT.domain.model.Parada;
import RadarSptrans.example.RadarSPT.domain.model.PosicaoLinha;
import RadarSptrans.example.RadarSPT.domain.model.PrevisaoParada;

import java.util.List;

public interface SpTransDadosPort {
    List<Linha> buscarLinhas(String termosBusca, String sessionCookie);

    PosicaoLinha buscarPosicaoLinha(int codigoLinha, String sessionCookie);

    /** Paradas da linha para as quais a SPTrans publica previsão em tempo real (não o itinerário completo). */
    List<Parada> buscarParadasComPrevisao(int codigoLinha, String sessionCookie);

    PrevisaoParada buscarPrevisao(long codigoParada, int codigoLinha, String sessionCookie);
}
