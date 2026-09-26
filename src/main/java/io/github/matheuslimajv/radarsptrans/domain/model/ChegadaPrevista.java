package io.github.matheuslimajv.radarsptrans.domain.model;

/**
 * Chegada prevista pela SPTrans de um veículo em uma parada.
 *
 * @param horario horário previsto (HH:mm, fuso de São Paulo)
 */
public record ChegadaPrevista(String prefixo, String horario, boolean acessivel) {
}
