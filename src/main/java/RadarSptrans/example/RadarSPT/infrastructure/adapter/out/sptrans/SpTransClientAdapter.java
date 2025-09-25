package RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans;

import RadarSptrans.example.RadarSPT.domain.model.LinhaResponse;
import RadarSptrans.example.RadarSPT.domain.model.PosicaoBusResponse;
import RadarSptrans.example.RadarSPT.domain.port.out.SpTransDadosPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SpTransClientAdapter implements SpTransDadosPort {

    private final SpTransClient spTransClient;

    public SpTransClientAdapter(SpTransClient spTransClient) {
        this.spTransClient = spTransClient;
    }

    @Override
    public List<LinhaResponse> buscarLinha(String termosBusca, String sessionCookie) {
        return spTransClient.buscarLinha(termosBusca, sessionCookie);
    }

    @Override
    public PosicaoBusResponse buscarPosicaoLinha(String codigoLinha, String sessionCookie) {
        return spTransClient.localBus(codigoLinha, sessionCookie);
    }
}
