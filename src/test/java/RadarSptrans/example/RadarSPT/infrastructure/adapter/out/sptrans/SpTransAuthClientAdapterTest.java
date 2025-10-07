package RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans;

import RadarSptrans.example.RadarSPT.domain.exception.AutenticacaoException;
import RadarSptrans.example.RadarSPT.domain.exception.CookieSessaoNaoEncontradoException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpTransAuthClientAdapterTest {

    private SpTransAuthClient authClient;
    private SpTransAuthClientAdapter adapter;

    @BeforeEach
    void setUp() {
        authClient = mock(SpTransAuthClient.class);
        adapter = new SpTransAuthClientAdapter(authClient, "token");
    }

    @Test
    void deveRetornarCookieSaneadoQuandoAutenticacaoSucesso() {
        Response response = criarResposta(200, Map.of("Set-Cookie", List.of("cookie=valor; Path=/; HttpOnly")));
        when(authClient.autenticar("token")).thenReturn(response);

        String cookie = adapter.autenticar();

        assertEquals("cookie=valor", cookie);
    }

    @Test
    void deveRetornarCookieMesmoQuandoFormatoNaoEhReconhecidoPeloParser() {
        Response response = criarResposta(200, Map.of("Set-Cookie", List.of("cookie=valor;secure")));
        when(authClient.autenticar("token")).thenReturn(response);

        String cookie = adapter.autenticar();

        assertEquals("cookie=valor", cookie);
    }

    @Test
    void deveLancarExcecaoQuandoCookieNaoEncontrado() {
        Response response = criarResposta(200, Map.of());
        when(authClient.autenticar("token")).thenReturn(response);

        CookieSessaoNaoEncontradoException exception = assertThrows(CookieSessaoNaoEncontradoException.class, adapter::autenticar);
        assertEquals("Cookie de sessão não encontrado na resposta de autenticação.", exception.getMessage());
    }

    @Test
    void deveLancarExcecaoDeAutenticacaoQuandoStatusNaoSucesso() {
        Response response = criarResposta(401, Map.of());
        when(authClient.autenticar("token")).thenReturn(response);

        AutenticacaoException exception = assertThrows(AutenticacaoException.class, adapter::autenticar);
        assertEquals("Falha ao autenticar com o serviço SPTrans.", exception.getMessage());
    }

    private Response criarResposta(int status, Map<String, Collection<String>> headers) {
        Request request = Request.create(Request.HttpMethod.POST,
                "https://api.olhovivo.sptrans.com.br/v2.1/Login/Autenticar",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null);
        return Response.builder()
                .status(status)
                .headers(headers)
                .request(request)
                .build();
    }
}
