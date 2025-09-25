package RadarSptrans.example.RadarSPT.application.service;

import RadarSptrans.example.RadarSPT.domain.model.LinhaResponse;
import RadarSptrans.example.RadarSPT.domain.model.PosicaoBusResponse;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarPosicaoPorCodigoUseCase;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarPosicaoPorTermoUseCase;
import RadarSptrans.example.RadarSPT.domain.port.out.AutenticacaoPort;
import RadarSptrans.example.RadarSPT.domain.port.out.SpTransDadosPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpTransApplicationService implements BuscarPosicaoPorCodigoUseCase, BuscarPosicaoPorTermoUseCase {

    private final AutenticacaoPort autenticacaoPort;
    private final SpTransDadosPort spTransDadosPort;

    public SpTransApplicationService(AutenticacaoPort autenticacaoPort, SpTransDadosPort spTransDadosPort) {
        this.autenticacaoPort = autenticacaoPort;
        this.spTransDadosPort = spTransDadosPort;
    }

    @Override
    public PosicaoBusResponse buscarPorTermo(String termosBusca, int indice) {
        String sessionCookie = autenticacaoPort.autenticar();
        List<LinhaResponse> linhas = spTransDadosPort.buscarLinha(termosBusca, sessionCookie);
        if (indice < 1 || indice > linhas.size()) {
            throw new IllegalArgumentException("Índice inválido.");
        }
        String codigoLinha = String.valueOf(linhas.get(indice - 1).getCl());
        return spTransDadosPort.buscarPosicaoLinha(codigoLinha, sessionCookie);
    }

    @Override
    public PosicaoBusResponse buscarPorCodigo(String codigoLinha) {
        String sessionCookie = autenticacaoPort.autenticar();
        return spTransDadosPort.buscarPosicaoLinha(codigoLinha, sessionCookie);
    }
}
