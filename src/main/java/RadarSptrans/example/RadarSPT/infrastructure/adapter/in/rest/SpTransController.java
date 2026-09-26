package RadarSptrans.example.RadarSPT.infrastructure.adapter.in.rest;

import RadarSptrans.example.RadarSPT.domain.model.LinhaResponse;
import RadarSptrans.example.RadarSPT.domain.model.PosicaoBusResponse;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarLinhasUseCase;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarPosicaoPorCodigoUseCase;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarPosicaoPorTermoUseCase;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sptrans")
public class SpTransController {

    private final BuscarPosicaoPorTermoUseCase buscarPosicaoPorTermoUseCase;
    private final BuscarPosicaoPorCodigoUseCase buscarPosicaoPorCodigoUseCase;
    private final BuscarLinhasUseCase buscarLinhasUseCase;

    public SpTransController(BuscarPosicaoPorTermoUseCase buscarPosicaoPorTermoUseCase,
                             BuscarPosicaoPorCodigoUseCase buscarPosicaoPorCodigoUseCase,
                             BuscarLinhasUseCase buscarLinhasUseCase) {
        this.buscarPosicaoPorTermoUseCase = buscarPosicaoPorTermoUseCase;
        this.buscarPosicaoPorCodigoUseCase = buscarPosicaoPorCodigoUseCase;
        this.buscarLinhasUseCase = buscarLinhasUseCase;
    }

    @GetMapping("/linhas")
    public List<LinhaResponse> listarLinhas(@RequestParam("termosBusca") @NotBlank String termosBusca) {
        return buscarLinhasUseCase.buscarLinhas(termosBusca);
    }

    @GetMapping("/buscar")
    public PosicaoBusResponse buscarLinhas(@RequestParam("termosBusca") @NotBlank String termosBusca,
                                           @RequestParam("indice") @Min(1) int indice) {
        return buscarPosicaoPorTermoUseCase.buscarPorTermo(termosBusca, indice);
    }

    @GetMapping("/posicao")
    public PosicaoBusResponse localBus(@RequestParam("codigoLinha") @Positive int codigoLinha) {
        return buscarPosicaoPorCodigoUseCase.buscarPorCodigo(codigoLinha);
    }
}
