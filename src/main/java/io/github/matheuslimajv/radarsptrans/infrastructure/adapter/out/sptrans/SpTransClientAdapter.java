package io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans;

import io.github.matheuslimajv.radarsptrans.config.CacheConfig;
import io.github.matheuslimajv.radarsptrans.domain.exception.SessaoExpiradaException;
import io.github.matheuslimajv.radarsptrans.domain.exception.SpTransIndisponivelException;
import io.github.matheuslimajv.radarsptrans.domain.model.Linha;
import io.github.matheuslimajv.radarsptrans.domain.model.Parada;
import io.github.matheuslimajv.radarsptrans.domain.model.PosicaoLinha;
import io.github.matheuslimajv.radarsptrans.domain.model.PrevisaoParada;
import io.github.matheuslimajv.radarsptrans.domain.port.out.SpTransDadosPort;
import feign.FeignException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

// As chaves de cache ignoram o cookie: o dado é o mesmo para qualquer sessão, e vários usuários
// olhando a mesma linha geram uma única chamada à SPTrans por janela de cache.
@Component
public class SpTransClientAdapter implements SpTransDadosPort {

    private final SpTransClient spTransClient;

    public SpTransClientAdapter(SpTransClient spTransClient) {
        this.spTransClient = spTransClient;
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.LINHAS, key = "#termosBusca.trim().toLowerCase()")
    public List<Linha> buscarLinhas(String termosBusca, String sessionCookie) {
        return executar(() -> SpTransMapper.linhas(spTransClient.buscarLinha(termosBusca, sessionCookie)));
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.POSICAO_LINHA, key = "#codigoLinha")
    public PosicaoLinha buscarPosicaoLinha(int codigoLinha, String sessionCookie) {
        return executar(() -> SpTransMapper.posicao(spTransClient.localBus(codigoLinha, sessionCookie)));
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.PARADAS_COM_PREVISAO, key = "#codigoLinha")
    public List<Parada> buscarParadasComPrevisao(int codigoLinha, String sessionCookie) {
        return executar(() -> SpTransMapper.paradas(spTransClient.buscarParadasPorLinha(codigoLinha, sessionCookie)));
    }

    @Override
    @Cacheable(cacheNames = CacheConfig.PREVISAO, key = "#codigoParada + ':' + #codigoLinha")
    public PrevisaoParada buscarPrevisao(long codigoParada, int codigoLinha, String sessionCookie) {
        return executar(() -> SpTransMapper.previsao(
                spTransClient.previsao(codigoParada, codigoLinha, sessionCookie), codigoLinha));
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
