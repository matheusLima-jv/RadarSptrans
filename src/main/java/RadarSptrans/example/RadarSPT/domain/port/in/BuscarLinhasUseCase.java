package RadarSptrans.example.RadarSPT.domain.port.in;

import RadarSptrans.example.RadarSPT.domain.model.Linha;

import java.util.List;

public interface BuscarLinhasUseCase {
    List<Linha> buscarLinhas(String termosBusca);
}
