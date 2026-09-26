package io.github.matheuslimajv.radarsptrans.domain.port.out;

public interface AutenticacaoPort {
    String autenticar();

    void invalidarSessao();
}
