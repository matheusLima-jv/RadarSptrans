package io.github.matheuslimajv.radarsptrans.domain.exception;

public class AutenticacaoException extends RuntimeException {

    public AutenticacaoException() {
        super("Falha ao autenticar com o serviço SPTrans.");
    }

    public AutenticacaoException(String message) {
        super(message);
    }
}
