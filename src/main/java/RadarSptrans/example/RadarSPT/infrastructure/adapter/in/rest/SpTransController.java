package RadarSptrans.example.RadarSPT.infrastructure.adapter.in.rest;

import RadarSptrans.example.RadarSPT.domain.model.PosicaoBusResponse;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarPosicaoPorCodigoUseCase;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarPosicaoPorTermoUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sptrans")
public class SpTransController {

    private final BuscarPosicaoPorTermoUseCase buscarPosicaoPorTermoUseCase;
    private final BuscarPosicaoPorCodigoUseCase buscarPosicaoPorCodigoUseCase;

    public SpTransController(BuscarPosicaoPorTermoUseCase buscarPosicaoPorTermoUseCase,
                             BuscarPosicaoPorCodigoUseCase buscarPosicaoPorCodigoUseCase) {
        this.buscarPosicaoPorTermoUseCase = buscarPosicaoPorTermoUseCase;
        this.buscarPosicaoPorCodigoUseCase = buscarPosicaoPorCodigoUseCase;
    }

    @GetMapping("/buscar")
    public PosicaoBusResponse buscarLinhas(@RequestParam("termosBusca") String termosBusca,
                                           @RequestParam("indice") int indice) {
        return buscarPosicaoPorTermoUseCase.buscarPorTermo(termosBusca, indice);
    }

    @GetMapping("/posicao")
    public PosicaoBusResponse localBus(@RequestParam("codigoLinha") String codigoLinha) {
        return buscarPosicaoPorCodigoUseCase.buscarPorCodigo(codigoLinha);
    }
}
