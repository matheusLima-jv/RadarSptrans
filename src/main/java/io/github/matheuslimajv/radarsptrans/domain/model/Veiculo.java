package io.github.matheuslimajv.radarsptrans.domain.model;

import java.time.Instant;

public record Veiculo(String prefixo, boolean acessivel, Instant atualizadoEm, double latitude, double longitude) {
}
