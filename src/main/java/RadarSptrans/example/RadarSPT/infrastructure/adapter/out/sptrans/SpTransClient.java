package RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans;

import RadarSptrans.example.RadarSPT.domain.model.LinhaResponse;
import RadarSptrans.example.RadarSPT.domain.model.PosicaoBusResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "spTransClient", url = "https://api.olhovivo.sptrans.com.br/v2.1")
public interface SpTransClient {

    @GetMapping("/Linha/Buscar")
    List<LinhaResponse> buscarLinha(@RequestParam("termosBusca") String termosBusca,
                                    @RequestHeader("Cookie") String sessionCookie);

    @GetMapping("/Posicao/Linha")
    PosicaoBusResponse localBus(@RequestParam("codigoLinha") String codigoLinha,
                                @RequestHeader("Cookie") String sessionCookie);
}
