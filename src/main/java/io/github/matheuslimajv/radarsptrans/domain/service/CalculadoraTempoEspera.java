package io.github.matheuslimajv.radarsptrans.domain.service;

import io.github.matheuslimajv.radarsptrans.domain.model.ChegadaEstimada;
import io.github.matheuslimajv.radarsptrans.domain.model.ChegadaPrevista;
import io.github.matheuslimajv.radarsptrans.domain.model.Itinerario;
import io.github.matheuslimajv.radarsptrans.domain.model.ParadaItinerario;
import io.github.matheuslimajv.radarsptrans.domain.model.PosicaoLinha;
import io.github.matheuslimajv.radarsptrans.domain.model.PrevisaoParada;
import io.github.matheuslimajv.radarsptrans.domain.model.Veiculo;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CalculadoraTempoEspera {

    // Ônibus a mais que isso da parada mais próxima do itinerário estão fora da rota (garagem, desvio).
    static final double DISTANCIA_MAXIMA_ROTA_METROS = 400;
    // Posições mais antigas que isso não representam onde o ônibus está.
    static final Duration IDADE_MAXIMA_POSICAO = Duration.ofMinutes(5);

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MINUTOS_DIA = 24 * 60;

    private CalculadoraTempoEspera() {
    }

    public static int indiceParadaMaisProxima(Itinerario itinerario, double latitude, double longitude) {
        List<ParadaItinerario> paradas = itinerario.paradas();
        int melhor = 0;
        double menorDistancia = Double.MAX_VALUE;
        for (int i = 0; i < paradas.size(); i++) {
            double distancia = distanciaMetros(paradas.get(i), latitude, longitude);
            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                melhor = i;
            }
        }
        return melhor;
    }

    public static double distanciaMetros(ParadaItinerario paradaItinerario, double latitude, double longitude) {
        return Distancia.metros(latitude, longitude,
                paradaItinerario.parada().latitude(), paradaItinerario.parada().longitude());
    }

    /** Converte a previsão oficial da SPTrans (horários HH:mm) em minutos a partir da hora de referência dela. */
    public static List<ChegadaEstimada> chegadasPrevistas(PrevisaoParada previsao) {
        LocalTime referencia = LocalTime.parse(previsao.horaReferencia(), HORA);
        return previsao.chegadas().stream()
                .map(chegada -> chegada(chegada.prefixo(), referencia,
                        minutosEntre(referencia, LocalTime.parse(chegada.horario(), HORA)), chegada.acessivel()))
                .sorted(Comparator.comparingInt(ChegadaEstimada::minutos))
                .toList();
    }

    /**
     * Estima chegadas pela posição dos ônibus: cada ônibus é associado à parada do itinerário mais próxima
     * dele; os que estão na parada alvo ou antes dela chegam após o tempo de percurso programado entre as duas.
     * Limitação: numa linha que passa duas vezes pelo mesmo lugar (laço), a associação pode errar o trecho.
     */
    public static List<ChegadaEstimada> chegadasPorPosicao(Itinerario itinerario, int indiceAlvo,
                                                           PosicaoLinha posicao, Instant agora) {
        LocalTime referencia = LocalTime.parse(posicao.horaReferencia(), HORA);
        int segundosAlvo = itinerario.paradas().get(indiceAlvo).segundosDesdeInicio();
        List<ChegadaEstimada> chegadas = new ArrayList<>();
        for (Veiculo veiculo : posicao.veiculos()) {
            if (veiculo.atualizadoEm() != null
                    && Duration.between(veiculo.atualizadoEm(), agora).compareTo(IDADE_MAXIMA_POSICAO) > 0) {
                continue;
            }
            int indiceVeiculo = indiceParadaMaisProxima(itinerario, veiculo.latitude(), veiculo.longitude());
            ParadaItinerario paradaVeiculo = itinerario.paradas().get(indiceVeiculo);
            if (indiceVeiculo > indiceAlvo
                    || distanciaMetros(paradaVeiculo, veiculo.latitude(), veiculo.longitude())
                    > DISTANCIA_MAXIMA_ROTA_METROS) {
                continue;
            }
            int minutos = Math.round((segundosAlvo - paradaVeiculo.segundosDesdeInicio()) / 60f);
            chegadas.add(chegada(veiculo.prefixo(), referencia, minutos, veiculo.acessivel()));
        }
        chegadas.sort(Comparator.comparingInt(ChegadaEstimada::minutos));
        return chegadas;
    }

    private static ChegadaEstimada chegada(String prefixo, LocalTime referencia, int minutos, boolean acessivel) {
        return new ChegadaEstimada(prefixo, minutos, referencia.plusMinutes(minutos).format(HORA), acessivel);
    }

    // Trata a virada da meia-noite (referência 23:58, previsão 00:05) sem transformar um atraso
    // de um minuto (previsão 23:57) em 23 horas.
    private static int minutosEntre(LocalTime referencia, LocalTime previsto) {
        int diferenca = (previsto.toSecondOfDay() - referencia.toSecondOfDay()) / 60;
        if (diferenca < -MINUTOS_DIA / 2) {
            diferenca += MINUTOS_DIA;
        }
        return Math.max(0, diferenca);
    }

    public static Double intervaloMedioMinutos(List<ChegadaEstimada> chegadas) {
        if (chegadas.size() < 2) {
            return null;
        }
        int total = chegadas.get(chegadas.size() - 1).minutos() - chegadas.get(0).minutos();
        return arredondar(total / (double) (chegadas.size() - 1));
    }

    public static Double arredondar(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}
