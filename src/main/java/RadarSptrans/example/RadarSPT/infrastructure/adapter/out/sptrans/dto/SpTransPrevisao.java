package RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans.dto;

import java.util.List;

/** Resposta de /Previsao; "p" vem null quando não há previsão da linha para a parada. */
public record SpTransPrevisao(String hr, Parada p) {

    public record Parada(long cp, String np, double py, double px, List<Linha> l) {
    }

    public record Linha(String c, int cl, int sl, List<Veiculo> vs) {
    }

    public record Veiculo(String p, String t, boolean a) {
    }
}
