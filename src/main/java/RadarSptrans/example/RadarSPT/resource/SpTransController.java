package RadarSptrans.example.RadarSPT.resource;

import RadarSptrans.example.RadarSPT.client.SpTransAuthClient;
import RadarSptrans.example.RadarSPT.client.SpTransClient;
import RadarSptrans.example.RadarSPT.response.LinhaResponse;
import RadarSptrans.example.RadarSPT.response.PosicaoBusResponse;
import feign.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/sptrans")
public class SpTransController {

    @Autowired
    private SpTransAuthClient authClient;

    @Autowired
    private SpTransClient spTransClient;

    private String sessionCookie;

    private final String token = "0b2b7996a6aae16fba7fd5386b3c630d5efc26cb0f9373e1e21aebe89ca60963";

    @GetMapping("/buscar")
    public PosicaoBusResponse buscarLinhas(@RequestParam("termosBusca") String termosBusca,
                                           @RequestParam("indice") int indice) {
        Response authResponse = authClient.autenticar(token);
        if (authResponse.status() == 200) {
            Collection<String> cookies = authResponse.headers().get("Set-Cookie");
            if (cookies != null && !cookies.isEmpty()) {
                sessionCookie = cookies.iterator().next(); //Armazena o cookie
            } else {
                throw new RuntimeException("Falha ao capturar o cookie de sessão.");
            }

            List<LinhaResponse> linhas = spTransClient.buscarLinha(termosBusca, sessionCookie);

            // *Valida o índice para evitar exceções
            if (indice < 1 || indice > linhas.size()) {
                throw new IllegalArgumentException("Índice inválido.");
            }

            // Retorna a linha correspondente ao índice (subtraindo 1 para corresponder ao índice da lista)
            return spTransClient.localBus(String.valueOf(linhas.get(indice - 1).getCl()), sessionCookie);
        } else {
            throw new RuntimeException("Autenticação falhou.");
        }
    }
    public PosicaoBusResponse localBus(@RequestParam("codigoLinha") String codigoLinha){
        Response authResponse = authClient.autenticar(token);
        if (authResponse.status() == 200) {
            Collection<String> cookies = authResponse.headers().get("Set-Cookie");
            if (cookies != null && !cookies.isEmpty()) {
                sessionCookie = cookies.iterator().next(); // Armazena o cookie
            } else {
                throw new RuntimeException("Falha ao capturar o cookie de sessão.");
            }

            return spTransClient.localBus(codigoLinha, sessionCookie);
        } else {
            throw new RuntimeException("Autenticação falhou.");
        }
    }
}

