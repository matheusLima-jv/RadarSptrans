package RadarSptrans.example.RadarSPT.domain.port.in;

import RadarSptrans.example.RadarSPT.domain.model.PosicaoBusResponse;

public interface BuscarPosicaoPorTermoUseCase {
    PosicaoBusResponse buscarPorTermo(String termosBusca, int indice);
}
