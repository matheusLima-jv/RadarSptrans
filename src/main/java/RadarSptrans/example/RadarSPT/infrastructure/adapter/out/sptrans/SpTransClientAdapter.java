package RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans;

import RadarSptrans.example.RadarSPT.domain.exception.SessaoExpiradaException;
import RadarSptrans.example.RadarSPT.domain.exception.SpTransIndisponivelException;
import RadarSptrans.example.RadarSPT.domain.model.LinhaResponse;
import RadarSptrans.example.RadarSPT.domain.model.PosicaoBusResponse;
import RadarSptrans.example.RadarSPT.domain.port.out.SpTransDadosPort;
import feign.FeignException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Component
public class SpTransClientAdapter implements SpTransDadosPort {

    private final SpTransClient spTransClient;

    public SpTransClientAdapter(SpTransClient spTransClient) {
        this.spTransClient = spTransClient;
    }

    @Override
    public List<LinhaResponse> buscarLinha(String termosBusca, String sessionCookie) {
        return executar(() -> spTransClient.buscarLinha(termosBusca, CookieSanitizer.sanitize(sessionCookie)));
    }

    @Override
    public PosicaoBusResponse buscarPosicaoLinha(int codigoLinha, String sessionCookie) {
        return executar(() -> spTransClient.localBus(codigoLinha, CookieSanitizer.sanitize(sessionCookie)));
    }

    // Traduz erros do Feign para exceções de domínio, mantendo o Feign fora das camadas internas.
    private <T> T executar(Supplier<T> chamada) {
        try {
            return chamada.get();
        } catch (FeignException.Unauthorized exception) {
            throw new SessaoExpiradaException();
        } catch (FeignException exception) {
            throw new SpTransIndisponivelException("Falha ao consultar a SPTrans (HTTP " + exception.status() + ").",
                    exception);
        }
    }
}
