package RadarSptrans.example.RadarSPT.domain.model;

import java.util.List;

/**
 * @param horaReferencia horário (HH:mm, fuso de São Paulo) em que a SPTrans gerou as posições
 */
public record PosicaoLinha(String horaReferencia, List<Veiculo> veiculos) {
}
