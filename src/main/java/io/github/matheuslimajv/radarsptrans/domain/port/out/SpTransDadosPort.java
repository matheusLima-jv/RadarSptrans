package io.github.matheuslimajv.radarsptrans.domain.port.out;

import io.github.matheuslimajv.radarsptrans.domain.model.Linha;
import io.github.matheuslimajv.radarsptrans.domain.model.Parada;
import io.github.matheuslimajv.radarsptrans.domain.model.PosicaoLinha;
import io.github.matheuslimajv.radarsptrans.domain.model.PrevisaoParada;

import java.util.List;

public interface SpTransDadosPort {
    List<Linha> buscarLinhas(String termosBusca, String sessionCookie);

    PosicaoLinha buscarPosicaoLinha(int codigoLinha, String sessionCookie);

    /** Paradas da linha para as quais a SPTrans publica previsão em tempo real (não o itinerário completo). */
    List<Parada> buscarParadasComPrevisao(int codigoLinha, String sessionCookie);

    PrevisaoParada buscarPrevisao(long codigoParada, int codigoLinha, String sessionCookie);
}
