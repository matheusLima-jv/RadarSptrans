package RadarSptrans.example.RadarSPT.domain.port.in;

import RadarSptrans.example.RadarSPT.domain.model.PosicaoBusResponse;

public interface BuscarPosicaoPorCodigoUseCase {
    PosicaoBusResponse buscarPorCodigo(String codigoLinha);
}
