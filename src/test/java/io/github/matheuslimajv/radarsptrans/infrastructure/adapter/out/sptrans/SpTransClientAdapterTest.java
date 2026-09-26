package io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans;

import io.github.matheuslimajv.radarsptrans.domain.exception.SessaoExpiradaException;
import io.github.matheuslimajv.radarsptrans.domain.exception.SpTransIndisponivelException;
import io.github.matheuslimajv.radarsptrans.domain.model.Linha;
import io.github.matheuslimajv.radarsptrans.domain.model.PrevisaoParada;
import io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans.dto.SpTransLinha;
import io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans.dto.SpTransPrevisao;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpTransClientAdapterTest {

    private SpTransClient spTransClient;
    private SpTransClientAdapter adapter;

    @BeforeEach
    void setUp() {
        spTransClient = mock(SpTransClient.class);
        adapter = new SpTransClientAdapter(spTransClient);
    }

    @Test
    void deveConverterLinhasParaModeloDeDominio() {
        when(spTransClient.buscarLinha("8000", "cookie=valor"))
                .thenReturn(List.of(new SpTransLinha(1273, false, "8000", 1, 10, "PÇA. RAMOS DE AZEVEDO", "TERM. LAPA")));

        List<Linha> linhas = adapter.buscarLinhas("8000", "cookie=valor");

        assertEquals(List.of(new Linha(1273, "8000-10", 1, "TERM. LAPA", "PÇA. RAMOS DE AZEVEDO", false)), linhas);
    }

    @Test
    void deveRetornarPrevisaoVaziaQuandoLinhaNaoPassaNaParada() {
        when(spTransClient.previsao(260015039L, 504, "cookie=valor")).thenReturn(new SpTransPrevisao("19:26", null));

        PrevisaoParada previsao = adapter.buscarPrevisao(260015039L, 504, "cookie=valor");

        assertEquals(PrevisaoParada.vazia("19:26"), previsao);
    }

    @Test
    void deveLancarSessaoExpiradaQuandoSpTransRetorna401() {
        when(spTransClient.localBus(123, "cookie=valor")).thenThrow(erroFeign(401));

        assertThrows(SessaoExpiradaException.class, () -> adapter.buscarPosicaoLinha(123, "cookie=valor"));
    }

    @Test
    void deveLancarIndisponivelQuandoSpTransRetornaErro() {
        when(spTransClient.buscarLinha("linha", "cookie=valor")).thenThrow(erroFeign(503));

        assertThrows(SpTransIndisponivelException.class, () -> adapter.buscarLinhas("linha", "cookie=valor"));
    }

    private FeignException erroFeign(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "https://sptrans", Map.of(), null,
                StandardCharsets.UTF_8, null);
        Response response = Response.builder().status(status).reason("erro").request(request).headers(Map.of()).build();
        return FeignException.errorStatus("SpTransClient#chamada", response);
    }
}
