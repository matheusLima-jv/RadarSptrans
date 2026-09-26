package RadarSptrans.example.RadarSPT.domain.exception;

public class SpTransIndisponivelException extends RuntimeException {

    public SpTransIndisponivelException() {
        super("Serviço SPTrans indisponível ou retornou uma resposta inesperada.");
    }

    public SpTransIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
