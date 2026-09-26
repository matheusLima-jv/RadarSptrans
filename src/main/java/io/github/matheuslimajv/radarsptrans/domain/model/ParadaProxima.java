package io.github.matheuslimajv.radarsptrans.domain.model;

import java.util.List;

/**
 * @param linhas letreiros das linhas que passam pela parada, ex.: ["8000-10", "7545-10"]
 */
public record ParadaProxima(Parada parada, int distanciaMetros, List<String> linhas) {
}
