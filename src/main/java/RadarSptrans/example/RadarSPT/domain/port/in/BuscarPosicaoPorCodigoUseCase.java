package RadarSptrans.example.RadarSPT.domain.port.in;

import RadarSptrans.example.RadarSPT.domain.model.PosicaoLinha;

public interface BuscarPosicaoPorCodigoUseCase {
    PosicaoLinha buscarPorCodigo(int codigoLinha);
}
