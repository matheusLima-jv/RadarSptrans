package io.github.matheuslimajv.radarsptrans.infrastructure.adapter.in.rest;

import io.github.matheuslimajv.radarsptrans.domain.exception.AutenticacaoException;
import io.github.matheuslimajv.radarsptrans.domain.exception.CookieSessaoNaoEncontradoException;
import io.github.matheuslimajv.radarsptrans.domain.exception.DadosProgramadosIndisponiveisException;
import io.github.matheuslimajv.radarsptrans.domain.exception.IndiceLinhaInvalidoException;
import io.github.matheuslimajv.radarsptrans.domain.exception.SessaoExpiradaException;
import io.github.matheuslimajv.radarsptrans.domain.exception.SpTransIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// Falhas na integração com a SPTrans viram 502: o cliente da nossa API não tem como corrigi-las,
// então 401 (que sugere "faça login") seria enganoso.
@RestControllerAdvice
public class SpTransExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SpTransExceptionHandler.class);

    private static final String CODE_INDICE_INVALIDO = "LINHA_INDICE_INVALIDO";
    private static final String CODE_PARAMETRO_INVALIDO = "PARAMETRO_INVALIDO";
    private static final String CODE_AUTENTICACAO_FALHOU = "AUTENTICACAO_FALHOU";
    private static final String CODE_COOKIE_NAO_ENCONTRADO = "COOKIE_SESSAO_NAO_ENCONTRADO";
    private static final String CODE_SPTRANS_INDISPONIVEL = "SPTRANS_INDISPONIVEL";
    private static final String CODE_DADOS_PROGRAMADOS_INDISPONIVEIS = "DADOS_PROGRAMADOS_INDISPONIVEIS";

    @ExceptionHandler(IndiceLinhaInvalidoException.class)
    public ResponseEntity<ApiErrorResponse> handleIndiceLinhaInvalido(IndiceLinhaInvalidoException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, CODE_INDICE_INVALIDO, exception.getMessage());
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class})
    public ResponseEntity<ApiErrorResponse> handleParametroInvalido(Exception exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, CODE_PARAMETRO_INVALIDO, mensagemParametroInvalido(exception));
    }

    @ExceptionHandler(AutenticacaoException.class)
    public ResponseEntity<ApiErrorResponse> handleAutenticacao(AutenticacaoException exception) {
        log.error("Falha ao autenticar na SPTrans: {}", exception.getMessage());
        return buildResponse(HttpStatus.BAD_GATEWAY, CODE_AUTENTICACAO_FALHOU, exception.getMessage());
    }

    @ExceptionHandler(CookieSessaoNaoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleCookieNaoEncontrado(CookieSessaoNaoEncontradoException exception) {
        log.error("SPTrans não retornou cookie de sessão: {}", exception.getMessage());
        return buildResponse(HttpStatus.BAD_GATEWAY, CODE_COOKIE_NAO_ENCONTRADO, exception.getMessage());
    }

    @ExceptionHandler({SpTransIndisponivelException.class, SessaoExpiradaException.class})
    public ResponseEntity<ApiErrorResponse> handleSpTransIndisponivel(RuntimeException exception) {
        log.error("Falha ao consultar a SPTrans: {}", exception.getMessage(), exception.getCause());
        return buildResponse(HttpStatus.BAD_GATEWAY, CODE_SPTRANS_INDISPONIVEL, exception.getMessage());
    }

    // 503: condição temporária (GTFS ainda carregando após o startup ou download falhou).
    @ExceptionHandler(DadosProgramadosIndisponiveisException.class)
    public ResponseEntity<ApiErrorResponse> handleDadosProgramadosIndisponiveis(
            DadosProgramadosIndisponiveisException exception) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, CODE_DADOS_PROGRAMADOS_INDISPONIVEIS,
                exception.getMessage());
    }

    private String mensagemParametroInvalido(Exception exception) {
        if (exception instanceof MissingServletRequestParameterException missing) {
            return "Parâmetro obrigatório ausente: " + missing.getParameterName() + ".";
        }
        if (exception instanceof MethodArgumentTypeMismatchException mismatch) {
            return "Valor inválido para o parâmetro " + mismatch.getName() + ".";
        }
        return "Parâmetros da requisição inválidos.";
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, message));
    }
}
