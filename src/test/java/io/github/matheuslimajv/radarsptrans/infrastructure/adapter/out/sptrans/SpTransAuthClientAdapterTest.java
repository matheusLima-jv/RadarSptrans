package io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans;

import io.github.matheuslimajv.radarsptrans.domain.exception.AutenticacaoException;
import io.github.matheuslimajv.radarsptrans.domain.exception.CookieSessaoNaoEncontradoException;
import io.github.matheuslimajv.radarsptrans.domain.exception.SpTransIndisponivelException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpTransAuthClientAdapterTest {

    private SpTransAuthClient authClient;
    private SpTransAuthClientAdapter adapter;

    @BeforeEach
    void setUp() {
        authClient = mock(SpTransAuthClient.class);
        adapter = new SpTransAuthClientAdapter(authClient, "token", Duration.ofMinutes(20));
    }

    @Test
    void deveRetornarCookieSaneadoQuandoAutenticacaoSucesso() {
        Response response = criarResposta(200, Map.of("Set-Cookie", List.of("cookie=valor; Path=/; HttpOnly")));
        when(authClient.autenticar("token")).thenReturn(response);

        String cookie = adapter.autenticar();

        assertEquals("cookie=valor", cookie);
        verify(response).close();
    }

    @Test
    void deveRetornarCookieMesmoQuandoFormatoNaoEhReconhecidoPeloParser() {
        Response response = criarResposta(200, Map.of("Set-Cookie", List.of("cookie=valor;secure")));
        when(authClient.autenticar("token")).thenReturn(response);

        String cookie = adapter.autenticar();

        assertEquals("cookie=valor", cookie);
    }

    @Test
    void deveManterCookieSemAtributosQuandoAutenticacaoSucesso() {
        Response response = criarResposta(200, Map.of("Set-Cookie", List.of("session=abc123")));
        when(authClient.autenticar("token")).thenReturn(response);

        String cookie = adapter.autenticar();

        assertEquals("session=abc123", cookie);
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
    }

    @Test
    void deveReutilizarCookieEmCacheEnquantoSessaoValida() {
        Response response = criarResposta(200, Map.of("Set-Cookie", List.of("cookie=valor; Path=/")));
        when(authClient.autenticar("token")).thenReturn(response);

        assertEquals("cookie=valor", adapter.autenticar());
        assertEquals("cookie=valor", adapter.autenticar());

        verify(authClient, times(1)).autenticar("token");
    }

    @Test
    void deveAutenticarNovamenteQuandoSessaoExpira() {
        RelogioAjustavel relogio = new RelogioAjustavel(Instant.parse("2024-01-01T10:00:00Z"));
        adapter = new SpTransAuthClientAdapter(authClient, "token", Duration.ofMinutes(20), relogio);
        Response primeira = criarResposta(200, Map.of("Set-Cookie", List.of("cookie=primeiro")));
        Response segunda = criarResposta(200, Map.of("Set-Cookie", List.of("cookie=segundo")));
        when(authClient.autenticar("token")).thenReturn(primeira, segunda);

        assertEquals("cookie=primeiro", adapter.autenticar());
        relogio.avancar(Duration.ofMinutes(21));

        assertEquals("cookie=segundo", adapter.autenticar());
        verify(authClient, times(2)).autenticar("token");
    }

    @Test
    void deveAutenticarNovamenteAposInvalidarSessao() {
        Response primeira = criarResposta(200, Map.of("Set-Cookie", List.of("cookie=primeiro")));
        Response segunda = criarResposta(200, Map.of("Set-Cookie", List.of("cookie=segundo")));
        when(authClient.autenticar("token")).thenReturn(primeira, segunda);

        assertEquals("cookie=primeiro", adapter.autenticar());
        adapter.invalidarSessao();

        assertEquals("cookie=segundo", adapter.autenticar());
    }

    @Test
    void deveLancarAutenticacaoQuandoSpTransRecusaToken() {
        Response response = Response.builder()
                .status(200)
                .request(criarRequest())
                .headers(Map.of())
                .body("false", StandardCharsets.UTF_8)
                .build();
        when(authClient.autenticar("token")).thenReturn(response);

        AutenticacaoException exception = assertThrows(AutenticacaoException.class, adapter::autenticar);
        assertEquals("Token da API SPTrans recusado.", exception.getMessage());
    }

    @Test
    void deveLancarAutenticacaoSemChamarSpTransQuandoTokenNaoConfigurado() {
        adapter = new SpTransAuthClientAdapter(authClient, " ", Duration.ofMinutes(20));

        assertThrows(AutenticacaoException.class, adapter::autenticar);
        verify(authClient, never()).autenticar(anyString());
    }

    @Test
    void deveLancarIndisponivelQuandoFalhaDeRede() {
        when(authClient.autenticar("token"))
                .thenThrow(new RetryableException(-1, "Read timed out executing POST " + criarRequest().url(),
                        Request.HttpMethod.POST, (Long) null, criarRequest()));

        SpTransIndisponivelException exception = assertThrows(SpTransIndisponivelException.class, adapter::autenticar);
        assertNull(exception.getCause());
        assertFalse(exception.getMessage().contains("token"));
    }

    private Request criarRequest() {
        return Request.create(Request.HttpMethod.POST, "https://sptrans/Login/Autenticar?token=token", Map.of(), null,
                StandardCharsets.UTF_8, null);
    }

    private static final class RelogioAjustavel extends Clock {
        private Instant agora;

        private RelogioAjustavel(Instant agora) {
            this.agora = agora;
        }

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    private Response criarResposta(int status, Map<String, Collection<String>> headers) {
        Response response = mock(Response.class);
        when(response.status()).thenReturn(status);
        when(response.headers()).thenReturn(headers);
        return response;
    }
}
