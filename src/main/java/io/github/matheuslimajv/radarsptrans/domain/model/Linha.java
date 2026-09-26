package io.github.matheuslimajv.radarsptrans.domain.model;

/**
 * Linha de ônibus em um sentido específico.
 *
 * @param codigo   código interno da SPTrans (usado nas consultas de posição e previsão)
 * @param letreiro letreiro completo, ex.: "8000-10"
 * @param sentido  1 = terminal secundário → principal, 2 = principal → secundário (sentido da SPTrans)
 * @param origem   terminal de onde o ônibus sai neste sentido
 * @param destino  terminal para onde o ônibus vai neste sentido
 */
public record Linha(int codigo, String letreiro, int sentido, String origem, String destino, boolean circular) {
}
