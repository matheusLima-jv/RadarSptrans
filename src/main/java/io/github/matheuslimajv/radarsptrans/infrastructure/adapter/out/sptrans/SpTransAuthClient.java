package io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans;

import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "authClient", url = "${sptrans.api.url}/Login")
public interface SpTransAuthClient {

    @PostMapping("/Autenticar")
    Response autenticar(@RequestParam("token") String token);
}
