package RadarSptrans.example.RadarSPT.application.service;

import RadarSptrans.example.RadarSPT.domain.exception.IndiceLinhaInvalidoException;
import RadarSptrans.example.RadarSPT.domain.exception.SessaoExpiradaException;
import RadarSptrans.example.RadarSPT.domain.model.LinhaResponse;
import RadarSptrans.example.RadarSPT.domain.model.PosicaoBusResponse;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarLinhasUseCase;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarPosicaoPorCodigoUseCase;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarPosicaoPorTermoUseCase;
import RadarSptrans.example.RadarSPT.domain.port.out.AutenticacaoPort;
import RadarSptrans.example.RadarSPT.domain.port.out.SpTransDadosPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

@Service
public class SpTransApplicationService implements BuscarPosicaoPorCodigoUseCase, BuscarPosicaoPorTermoUseCase,
        BuscarLinhasUseCase {

    private final AutenticacaoPort autenticacaoPort;
    private final SpTransDadosPort spTransDadosPort;

    public SpTransApplicationService(AutenticacaoPort autenticacaoPort, SpTransDadosPort spTransDadosPort) {
        this.autenticacaoPort = autenticacaoPort;
        this.spTransDadosPort = spTransDadosPort;
    }

    @Override
    public List<LinhaResponse> buscarLinhas(String termosBusca) {
        List<LinhaResponse> linhas = comSessao(cookie -> spTransDadosPort.buscarLinha(termosBusca, cookie));
        return linhas != null ? linhas : List.of();
    }

    @Override
    public PosicaoBusResponse buscarPorTermo(String termosBusca, int indice) {
        List<LinhaResponse> linhas = buscarLinhas(termosBusca);
        if (indice < 1 || indice > linhas.size()) {
            throw new IndiceLinhaInvalidoException();
        }
        return buscarPorCodigo(linhas.get(indice - 1).getCl());
    }

    @Override
    public PosicaoBusResponse buscarPorCodigo(int codigoLinha) {
        return comSessao(cookie -> spTransDadosPort.buscarPosicaoLinha(codigoLinha, cookie));
    }

    // A sessão fica em cache no adaptador de autenticação; se a SPTrans recusar o cookie,
    // descarta a sessão e tenta uma única vez com um cookie novo.
    private <T> T comSessao(Function<String, T> chamada) {
        try {
            return chamada.apply(autenticacaoPort.autenticar());
        } catch (SessaoExpiradaException exception) {
            autenticacaoPort.invalidarSessao();
            return chamada.apply(autenticacaoPort.autenticar());
        }
    }
}
