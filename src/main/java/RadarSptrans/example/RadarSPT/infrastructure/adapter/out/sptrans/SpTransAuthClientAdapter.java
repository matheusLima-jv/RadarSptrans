package RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans;

import RadarSptrans.example.RadarSPT.domain.port.out.AutenticacaoPort;
import feign.Response;
import org.springframework.beans.factory.annotation.Value;
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
        Response authResponse = authClient.autenticar(apiToken);
        if (authResponse.status() == 200) {
            Collection<String> cookies = authResponse.headers().get("Set-Cookie");
            if (cookies != null && !cookies.isEmpty()) {
                return cookies.iterator().next();
            }
            throw new RuntimeException("Falha ao capturar o cookie de sessão.");
        }
        throw new RuntimeException("Autenticação falhou.");
    }
}
