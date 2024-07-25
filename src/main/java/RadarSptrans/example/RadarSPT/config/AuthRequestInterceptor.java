package RadarSptrans.example.RadarSPT.config;

import RadarSptrans.example.RadarSPT.service.AuthService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthRequestInterceptor implements RequestInterceptor {

    private final AuthService authService;

    @Override
    public void apply(RequestTemplate template) {
        String token = authService.getAuthToken(); // Supondo que o AuthService tenha um método getToken() que retorna o token atual.
        if (token != null) {
            template.header("Authorization", "Bearer " + token);
        }
    }
}

