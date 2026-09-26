package io.github.matheuslimajv.radarsptrans.domain.exception;

public class IndiceLinhaInvalidoException extends RuntimeException {

    public IndiceLinhaInvalidoException() {
        super("Índice de linha informado é inválido.");
    }

    public IndiceLinhaInvalidoException(String message) {
        super(message);
    }
}
