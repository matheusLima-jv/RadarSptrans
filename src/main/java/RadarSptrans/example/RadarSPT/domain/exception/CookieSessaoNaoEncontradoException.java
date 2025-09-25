package RadarSptrans.example.RadarSPT.domain.exception;

public class CookieSessaoNaoEncontradoException extends RuntimeException {

    public CookieSessaoNaoEncontradoException() {
        super("Cookie de sessão não encontrado na resposta de autenticação.");
    }

    public CookieSessaoNaoEncontradoException(String message) {
        super(message);
    }
}
