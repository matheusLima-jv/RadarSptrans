package RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans;

import RadarSptrans.example.RadarSPT.domain.exception.AutenticacaoException;
import RadarSptrans.example.RadarSPT.domain.exception.CookieSessaoNaoEncontradoException;
import RadarSptrans.example.RadarSPT.domain.port.out.AutenticacaoPort;
import feign.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.HttpCookie;
import java.util.Collection;
import java.util.List;

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
        try {
            if (authResponse.status() == HttpStatus.OK.value()) {
                Collection<String> cookies = authResponse.headers().get("Set-Cookie");
                if (cookies != null && !cookies.isEmpty()) {
                    String rawCookie = cookies.iterator().next();
                    String sanitizedCookie = CookieSanitizer.sanitize(rawCookie);
                    if (!sanitizedCookie.isEmpty()) {
                        return sanitizedCookie;
                    }
                }
                throw new CookieSessaoNaoEncontradoException();
            }
            throw new AutenticacaoException();
        } finally {
            authResponse.close();
        }
    }

    private String sanitizeCookie(String rawCookie) {
        try {
            List<HttpCookie> parsedCookies = HttpCookie.parse(rawCookie);
            if (!parsedCookies.isEmpty()) {
                HttpCookie cookie = parsedCookies.get(0);
                return cookie.getName() + "=" + cookie.getValue();
            }
        } catch (IllegalArgumentException ignored) {
            // Caso não seja possível realizar o parse, utiliza fallback manual abaixo.
        }
        int delimiterIndex = rawCookie.indexOf(';');
        return delimiterIndex >= 0 ? rawCookie.substring(0, delimiterIndex) : rawCookie;
    }
}
