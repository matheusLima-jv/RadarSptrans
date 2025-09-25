package RadarSptrans.example.RadarSPT.infrastructure.adapter.in.rest;

import RadarSptrans.example.RadarSPT.domain.exception.AutenticacaoException;
import RadarSptrans.example.RadarSPT.domain.exception.CookieSessaoNaoEncontradoException;
import RadarSptrans.example.RadarSPT.domain.exception.IndiceLinhaInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpTransExceptionHandlerTest {

    private SpTransExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SpTransExceptionHandler();
    }

    @Test
    void deveMapearIndiceLinhaInvalidoParaBadRequest() {
        ResponseEntity<ApiErrorResponse> response = handler.handleIndiceLinhaInvalido(new IndiceLinhaInvalidoException());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("LINHA_INDICE_INVALIDO", response.getBody().code());
        assertEquals("Índice de linha informado é inválido.", response.getBody().message());
    }

    @Test
    void deveMapearAutenticacaoParaUnauthorized() {
        ResponseEntity<ApiErrorResponse> response = handler.handleAutenticacao(new AutenticacaoException());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("AUTENTICACAO_FALHOU", response.getBody().code());
        assertEquals("Falha ao autenticar com o serviço SPTrans.", response.getBody().message());
    }

    @Test
    void deveMapearCookieNaoEncontradoParaUnauthorized() {
        ResponseEntity<ApiErrorResponse> response = handler.handleCookieNaoEncontrado(new CookieSessaoNaoEncontradoException());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("COOKIE_SESSAO_NAO_ENCONTRADO", response.getBody().code());
        assertEquals("Cookie de sessão não encontrado na resposta de autenticação.", response.getBody().message());
    }
}
