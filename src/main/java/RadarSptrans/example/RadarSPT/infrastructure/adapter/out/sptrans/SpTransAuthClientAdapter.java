package RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans;

import RadarSptrans.example.RadarSPT.domain.exception.AutenticacaoException;
import RadarSptrans.example.RadarSPT.domain.exception.CookieSessaoNaoEncontradoException;
import RadarSptrans.example.RadarSPT.domain.exception.SpTransIndisponivelException;
import RadarSptrans.example.RadarSPT.domain.port.out.AutenticacaoPort;
import feign.FeignException;
import feign.Response;
import feign.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

@Component
public class SpTransAuthClientAdapter implements AutenticacaoPort {

    private final SpTransAuthClient authClient;
    private final String apiToken;
    private final Duration sessaoTtl;
    private final Clock clock;

    private String cookieEmCache;
    private Instant cookieExpiraEm = Instant.MIN;

    @Autowired
    public SpTransAuthClientAdapter(SpTransAuthClient authClient,
                                    @Value("${sptrans.api.token}") String apiToken,
                                    @Value("${sptrans.api.session-ttl:PT20M}") Duration sessaoTtl) {
        this(authClient, apiToken, sessaoTtl, Clock.systemUTC());
    }

    SpTransAuthClientAdapter(SpTransAuthClient authClient, String apiToken, Duration sessaoTtl, Clock clock) {
        this.authClient = authClient;
        this.apiToken = apiToken;
        this.sessaoTtl = sessaoTtl;
        this.clock = clock;
    }

    // synchronized: requisições concorrentes com o cache vazio disparam um único login na SPTrans.
    @Override
    public synchronized String autenticar() {
        if (cookieEmCache != null && clock.instant().isBefore(cookieExpiraEm)) {
            return cookieEmCache;
        }
        cookieEmCache = criarSessao();
        cookieExpiraEm = clock.instant().plus(sessaoTtl);
        return cookieEmCache;
    }

    @Override
    public synchronized void invalidarSessao() {
        cookieEmCache = null;
    }

    private String criarSessao() {
        if (!StringUtils.hasText(apiToken)) {
            throw new AutenticacaoException("Token da API SPTrans não configurado (defina SPTRANS_API_TOKEN).");
        }

        Response authResponse;
        try {
            authResponse = authClient.autenticar(apiToken);
        } catch (FeignException exception) {
            // Sem a causa de propósito: a mensagem do Feign traz a URL com o token na query string.
            throw new SpTransIndisponivelException("Falha de comunicação ao autenticar com a SPTrans ("
                    + exception.getClass().getSimpleName() + ").", null);
        }

        try {
            if (authResponse.status() != HttpStatus.OK.value()) {
                throw new AutenticacaoException();
            }
            // A SPTrans responde 200 com corpo "false" quando o token é inválido.
            if ("false".equalsIgnoreCase(lerCorpo(authResponse))) {
                throw new AutenticacaoException("Token da API SPTrans recusado.");
            }
            Collection<String> cookies = authResponse.headers().get("Set-Cookie");
            if (cookies != null && !cookies.isEmpty()) {
                String sanitizedCookie = CookieSanitizer.sanitize(cookies.iterator().next());
                if (!sanitizedCookie.isEmpty()) {
                    return sanitizedCookie;
                }
            }
            throw new CookieSessaoNaoEncontradoException();
        } finally {
            authResponse.close();
        }
    }

    private String lerCorpo(Response response) {
        if (response.body() == null) {
            return "";
        }
        try {
            return Util.toString(response.body().asReader(StandardCharsets.UTF_8)).trim();
        } catch (IOException exception) {
            return "";
        }
    }
}
