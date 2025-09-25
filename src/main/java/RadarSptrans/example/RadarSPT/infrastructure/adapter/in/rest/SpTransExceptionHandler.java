package RadarSptrans.example.RadarSPT.infrastructure.adapter.in.rest;

import RadarSptrans.example.RadarSPT.domain.exception.AutenticacaoException;
import RadarSptrans.example.RadarSPT.domain.exception.CookieSessaoNaoEncontradoException;
import RadarSptrans.example.RadarSPT.domain.exception.IndiceLinhaInvalidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SpTransExceptionHandler {

    private static final String CODE_INDICE_INVALIDO = "LINHA_INDICE_INVALIDO";
    private static final String CODE_AUTENTICACAO_FALHOU = "AUTENTICACAO_FALHOU";
    private static final String CODE_COOKIE_NAO_ENCONTRADO = "COOKIE_SESSAO_NAO_ENCONTRADO";

    @ExceptionHandler(IndiceLinhaInvalidoException.class)
    public ResponseEntity<ApiErrorResponse> handleIndiceLinhaInvalido(IndiceLinhaInvalidoException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, CODE_INDICE_INVALIDO, exception.getMessage());
    }

    @ExceptionHandler(AutenticacaoException.class)
    public ResponseEntity<ApiErrorResponse> handleAutenticacao(AutenticacaoException exception) {
        return buildResponse(HttpStatus.UNAUTHORIZED, CODE_AUTENTICACAO_FALHOU, exception.getMessage());
    }

    @ExceptionHandler(CookieSessaoNaoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleCookieNaoEncontrado(CookieSessaoNaoEncontradoException exception) {
        return buildResponse(HttpStatus.UNAUTHORIZED, CODE_COOKIE_NAO_ENCONTRADO, exception.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, message));
    }
}
