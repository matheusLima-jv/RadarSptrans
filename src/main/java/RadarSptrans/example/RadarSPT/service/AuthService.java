package RadarSptrans.example.RadarSPT.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthService {

    @Autowired
    private RestTemplate restTemplate;

    private String authToken;

    @PostConstruct
    public void authenticate() {
        String apiKey = "ebeebd3546ba4a353409e2fd9df94acb827e18520f44b4d657f83caedd4f0ec0"; // Substitua pela sua chave de API
        String url = "http://api.olhovivo.sptrans.com.br/v2.1/Login/Autenticar?token=" + apiKey;

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
            if (response.getStatusCode() == HttpStatus.OK && "true".equals(response.getBody())) {
                // Autenticação bem-sucedida
                this.authToken = apiKey; // Use a chave de API como token
            } else {
                throw new RuntimeException("Failed to authenticate with SPTrans API");
            }
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Failed to authenticate with SPTrans API: " + e.getMessage());
        }
    }

    public String getAuthToken() {
        return authToken;
    }
}