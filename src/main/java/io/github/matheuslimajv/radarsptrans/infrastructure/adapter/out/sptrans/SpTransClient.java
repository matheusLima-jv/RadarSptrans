package io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans;

import io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans.dto.SpTransLinha;
import io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans.dto.SpTransParada;
import io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans.dto.SpTransPosicaoLinha;
import io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans.dto.SpTransPrevisao;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "spTransClient", url = "${sptrans.api.url}", configuration = SpTransClientConfig.class)
public interface SpTransClient {

    @GetMapping("/Linha/Buscar")
    List<SpTransLinha> buscarLinha(@RequestParam("termosBusca") String termosBusca,
                                   @RequestHeader("Cookie") String sessionCookie);

    @GetMapping("/Posicao/Linha")
    SpTransPosicaoLinha localBus(@RequestParam("codigoLinha") int codigoLinha,
                                 @RequestHeader("Cookie") String sessionCookie);

    @GetMapping("/Parada/BuscarParadasPorLinha")
    List<SpTransParada> buscarParadasPorLinha(@RequestParam("codigoLinha") int codigoLinha,
                                              @RequestHeader("Cookie") String sessionCookie);

    @GetMapping("/Previsao")
    SpTransPrevisao previsao(@RequestParam("codigoParada") long codigoParada,
                             @RequestParam("codigoLinha") int codigoLinha,
                             @RequestHeader("Cookie") String sessionCookie);
}
