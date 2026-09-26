package io.github.matheuslimajv.radarsptrans.domain.exception;

public class ItinerarioNaoEncontradoException extends RuntimeException {

    public ItinerarioNaoEncontradoException(String letreiro, int sentido) {
        super("Itinerário não encontrado para a linha " + letreiro + " no sentido " + sentido + ".");
    }
}
