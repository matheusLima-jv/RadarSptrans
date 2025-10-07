package RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans;

import RadarSptrans.example.RadarSPT.domain.exception.AutenticacaoException;
import RadarSptrans.example.RadarSPT.domain.exception.CookieSessaoNaoEncontradoException;
import RadarSptrans.example.RadarSPT.domain.port.out.AutenticacaoPort;
import feign.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class SpTransAuthClientAdapter implements AutenticacaoPort {

    private final SpTransAuthClient authClient;
    private final String apiToken;

    public SpTransAuthClientAdapter(SpTransAuthClient authClient,
                                    @Value("${sptrans.api.token}") String apiToken) {
        this.authClient = authClient;
        this.apiToken = apiToken;
    }

    @Override
    public String autenticar() {
        try (Response authResponse = authClient.autenticar(apiToken)) {
            if (authResponse.status() == HttpStatus.OK.value()) {
                Collection<String> cookies = authResponse.headers().get("Set-Cookie");
                if (cookies != null && !cookies.isEmpty()) {
                    return cookies.iterator().next();
                }
                throw new CookieSessaoNaoEncontradoException();
            }
            throw new AutenticacaoException();
        }
    }
}
