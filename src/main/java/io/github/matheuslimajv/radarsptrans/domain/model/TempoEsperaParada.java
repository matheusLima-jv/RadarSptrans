package io.github.matheuslimajv.radarsptrans.domain.model;

import java.util.List;

/**
 * Quanto se espera por uma linha na parada dela mais próxima de um ponto.
 *
 * @param intervaloProgramadoMinutos      intervalo entre ônibus previsto no GTFS para o horário atual;
 *                                        null se a linha não opera agora
 * @param esperaMediaMinutos              espera média para quem chega à parada sem olhar o horário
 *                                        (metade do intervalo programado)
 * @param chegadas                        próximas chegadas em tempo real, da mais próxima para a mais distante
 * @param intervaloObservadoMinutos       intervalo médio entre as chegadas em tempo real; null com menos de 2
 * @param fonteChegadas                   de onde vieram as chegadas em tempo real
 * @param horaReferencia                  hora (HH:mm) dos dados em tempo real da SPTrans; null sem dados
 */
public record TempoEsperaParada(Linha linha,
                                Parada parada,
                                int distanciaMetros,
                                Double intervaloProgramadoMinutos,
                                Double esperaMediaMinutos,
                                List<ChegadaEstimada> chegadas,
                                Integer proximaChegadaMinutos,
                                Double intervaloObservadoMinutos,
                                FonteChegadas fonteChegadas,
                                String horaReferencia) {
}
