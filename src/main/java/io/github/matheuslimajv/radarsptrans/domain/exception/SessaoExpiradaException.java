package io.github.matheuslimajv.radarsptrans.domain.exception;

public class SessaoExpiradaException extends RuntimeException {

    public SessaoExpiradaException() {
        super("Sessão com o serviço SPTrans expirou ou foi recusada.");
    }

    public SessaoExpiradaException(String message) {
        super(message);
    }
}
