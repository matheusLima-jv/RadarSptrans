package RadarSptrans.example.RadarSPT.domain.port.in;

import RadarSptrans.example.RadarSPT.domain.model.PosicaoLinha;

public interface BuscarPosicaoPorTermoUseCase {
    PosicaoLinha buscarPorTermo(String termosBusca, int indice);
}
