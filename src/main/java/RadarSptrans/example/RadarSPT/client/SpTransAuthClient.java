package RadarSptrans.example.RadarSPT.client;

import feign.Headers;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "authClient", url = "https://api.olhovivo.sptrans.com.br/v2.1/Login")
public interface SpTransAuthClient {

    @PostMapping("/Autenticar")
    @Headers("Content-Type: application/x-www-form-urlencoded")
    Response autenticar(@RequestParam("token") String token);
}