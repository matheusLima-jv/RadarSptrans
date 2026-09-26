package RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans;

import RadarSptrans.example.RadarSPT.domain.exception.SessaoExpiradaException;
import RadarSptrans.example.RadarSPT.domain.exception.SpTransIndisponivelException;
import RadarSptrans.example.RadarSPT.domain.model.LinhaResponse;
import RadarSptrans.example.RadarSPT.domain.model.PosicaoBusResponse;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
    void deveEnviarCookieSaneadoNaBuscaDeLinhas() {
        List<LinhaResponse> expected = List.of();
        when(spTransClient.buscarLinha(eq("linha"), eq("cookie=valor"))).thenReturn(expected);

        List<LinhaResponse> result = adapter.buscarLinha("linha", "cookie=valor; Path=/; HttpOnly");

        assertSame(expected, result);
        verify(spTransClient).buscarLinha("linha", "cookie=valor");
    }

    @Test
    void deveEnviarCookieSaneadoNaBuscaDePosicao() {
        PosicaoBusResponse expected = new PosicaoBusResponse();
        when(spTransClient.localBus(eq(123), eq("cookie=valor"))).thenReturn(expected);

        PosicaoBusResponse result = adapter.buscarPosicaoLinha(123, "cookie=valor; Secure");

        assertSame(expected, result);
        verify(spTransClient).localBus(123, "cookie=valor");
    }

    @Test
    void deveLancarSessaoExpiradaQuandoSpTransRetorna401() {
        when(spTransClient.localBus(eq(123), eq("cookie=valor"))).thenThrow(erroFeign(401));

        assertThrows(SessaoExpiradaException.class, () -> adapter.buscarPosicaoLinha(123, "cookie=valor"));
    }

    @Test
    void deveLancarIndisponivelQuandoSpTransRetornaErro() {
        when(spTransClient.buscarLinha(eq("linha"), eq("cookie=valor"))).thenThrow(erroFeign(503));

        assertThrows(SpTransIndisponivelException.class, () -> adapter.buscarLinha("linha", "cookie=valor"));
    }

    private FeignException erroFeign(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "https://sptrans", Map.of(), null,
                StandardCharsets.UTF_8, null);
        Response response = Response.builder().status(status).reason("erro").request(request).headers(Map.of()).build();
        return FeignException.errorStatus("SpTransClient#chamada", response);
    }
}
