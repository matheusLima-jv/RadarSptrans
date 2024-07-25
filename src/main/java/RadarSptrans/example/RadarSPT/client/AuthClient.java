package RadarSptrans.example.RadarSPT.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="auth-client", url="http://api.olhovivo.sptrans.com.br/v2.1")
public interface AuthClient {

    @PostMapping(value = "/Login/Autenticar")
    void authenticate(@RequestParam("token") String token);
}
