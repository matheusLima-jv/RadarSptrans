package io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans.dto;

import java.time.Instant;

// O prefixo "p" chega como texto em /Posicao/Linha e como número em /Posicao; o Jackson converte ambos.
public record SpTransVeiculo(String p, boolean a, Instant ta, double py, double px) {
}
