package RadarSptrans.example.RadarSPT.domain.exception;

public class DadosProgramadosIndisponiveisException extends RuntimeException {

    public DadosProgramadosIndisponiveisException() {
        super("Dados programados (GTFS) da SPTrans ainda não foram carregados. Tente novamente em instantes.");
    }
}
