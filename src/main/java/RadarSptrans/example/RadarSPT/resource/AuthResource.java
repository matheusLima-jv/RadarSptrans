package RadarSptrans.example.RadarSPT.resource;

import RadarSptrans.example.RadarSPT.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthResource {

    private final AuthService authService;

    @GetMapping("/test-auth")
    public String testAuth() {
        authService.authenticate();
        return "Authenticated";
    }
}
