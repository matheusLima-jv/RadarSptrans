package io.github.matheuslimajv.radarsptrans.application.service;

import io.github.matheuslimajv.radarsptrans.domain.exception.IndiceLinhaInvalidoException;
import io.github.matheuslimajv.radarsptrans.domain.model.Linha;
import io.github.matheuslimajv.radarsptrans.domain.model.PosicaoLinha;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarLinhasUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarPosicaoPorCodigoUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarPosicaoPorTermoUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.out.SpTransDadosPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpTransApplicationService implements BuscarPosicaoPorCodigoUseCase, BuscarPosicaoPorTermoUseCase,
        BuscarLinhasUseCase {

    private final SessaoSpTrans sessao;
    private final SpTransDadosPort spTransDadosPort;

    public SpTransApplicationService(SessaoSpTrans sessao, SpTransDadosPort spTransDadosPort) {
        this.sessao = sessao;
        this.spTransDadosPort = spTransDadosPort;
    }

    @Override
    public List<Linha> buscarLinhas(String termosBusca) {
        List<Linha> linhas = sessao.executar(cookie -> spTransDadosPort.buscarLinhas(termosBusca, cookie));
        return linhas != null ? linhas : List.of();
    }

    @Override
    public PosicaoLinha buscarPorTermo(String termosBusca, int indice) {
        List<Linha> linhas = buscarLinhas(termosBusca);
        if (indice < 1 || indice > linhas.size()) {
            throw new IndiceLinhaInvalidoException();
        }
        return buscarPorCodigo(linhas.get(indice - 1).codigo());
    }

    @Override
    public PosicaoLinha buscarPorCodigo(int codigoLinha) {
        return sessao.executar(cookie -> spTransDadosPort.buscarPosicaoLinha(codigoLinha, cookie));
    }
}
