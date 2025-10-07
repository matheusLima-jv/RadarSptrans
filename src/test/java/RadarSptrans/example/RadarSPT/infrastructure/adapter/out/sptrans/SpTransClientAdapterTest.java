package RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans;

import RadarSptrans.example.RadarSPT.domain.model.LinhaResponse;
import RadarSptrans.example.RadarSPT.domain.model.PosicaoBusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
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
        when(spTransClient.localBus(eq("123"), eq("cookie=valor"))).thenReturn(expected);

        PosicaoBusResponse result = adapter.buscarPosicaoLinha("123", "cookie=valor; Secure");

        assertSame(expected, result);
        verify(spTransClient).localBus("123", "cookie=valor");
    }
}
