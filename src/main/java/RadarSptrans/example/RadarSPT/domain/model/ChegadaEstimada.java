package RadarSptrans.example.RadarSPT.domain.model;

/**
 * @param minutos minutos até o ônibus chegar à parada, contados a partir da hora de referência da SPTrans
 * @param horario horário estimado (HH:mm, fuso de São Paulo)
 */
public record ChegadaEstimada(String prefixo, int minutos, String horario, boolean acessivel) {
}
