package io.github.matheuslimajv.radarsptrans.domain.model;

/**
 * Faixa horária com intervalo programado entre ônibus (frequencies.txt do GTFS).
 * Os horários são segundos desde a meia-noite e podem passar de 24h, como permite o GTFS.
 */
public record JanelaFrequencia(int inicioSegundos, int fimSegundos, int intervaloSegundos) {

    private static final int UM_DIA = 24 * 60 * 60;

    // O GTFS da SPTrans usa janelas como 07:00:00-07:59:00; o fim vale até o último segundo desse minuto.
    public boolean contem(int segundosDoDia) {
        return dentro(segundosDoDia) || dentro(segundosDoDia + UM_DIA);
    }

    private boolean dentro(int segundos) {
        return segundos >= inicioSegundos && segundos <= fimSegundos + 59;
    }
}
