package RadarSptrans.example.RadarSPT.client;

import RadarSptrans.example.RadarSPT.config.FeignConfig;
import RadarSptrans.example.RadarSPT.response.BusInfo;
import RadarSptrans.example.RadarSPT.response.LinhasLocais;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="client-radar",url="http://api.olhovivo.sptrans.com.br/v2.1", configuration = FeignConfig.class)
public interface RadarClient {

    @GetMapping(value = "/Linha/Buscar")
    BusInfo[] busInfo(@RequestParam("termosBusca") String bus_id);

    @GetMapping(value = "/Posicao/Linha")
    LinhasLocais linhasLocais(@RequestParam("codigoLinha") int cl);


}
