package RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans;

import RadarSptrans.example.RadarSPT.domain.exception.AutenticacaoException;
import RadarSptrans.example.RadarSPT.domain.exception.CookieSessaoNaoEncontradoException;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
    void deveRetornarCookieQuandoAutenticacaoSucesso() {
        Response response = mock(Response.class);
        when(response.status()).thenReturn(200);
        when(response.headers()).thenReturn(Map.of("Set-Cookie", List.of("cookie=valor")));
        when(authClient.autenticar("token")).thenReturn(response);

        String cookie = adapter.autenticar();

        assertEquals("cookie=valor", cookie);
        verify(response).close();
    }

    @Test
    void deveLancarExcecaoQuandoCookieNaoEncontrado() {
        Response response = mock(Response.class);
        when(response.status()).thenReturn(200);
        when(response.headers()).thenReturn(Collections.emptyMap());
        when(authClient.autenticar("token")).thenReturn(response);

        CookieSessaoNaoEncontradoException exception = assertThrows(CookieSessaoNaoEncontradoException.class, adapter::autenticar);
        assertEquals("Cookie de sessão não encontrado na resposta de autenticação.", exception.getMessage());
        verify(response).close();
    }

    @Test
    void deveLancarExcecaoDeAutenticacaoQuandoStatusNaoSucesso() {
        Response response = mock(Response.class);
        when(response.status()).thenReturn(401);
        when(authClient.autenticar("token")).thenReturn(response);

        AutenticacaoException exception = assertThrows(AutenticacaoException.class, adapter::autenticar);
        assertEquals("Falha ao autenticar com o serviço SPTrans.", exception.getMessage());
        verify(response).close();
    }
}
