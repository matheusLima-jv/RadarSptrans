package RadarSptrans.example.RadarSPT.application.service;

import RadarSptrans.example.RadarSPT.domain.exception.IndiceLinhaInvalidoException;
import RadarSptrans.example.RadarSPT.domain.exception.SessaoExpiradaException;
import RadarSptrans.example.RadarSPT.domain.model.LinhaResponse;
import RadarSptrans.example.RadarSPT.domain.model.PosicaoBusResponse;
import RadarSptrans.example.RadarSPT.domain.port.out.AutenticacaoPort;
import RadarSptrans.example.RadarSPT.domain.port.out.SpTransDadosPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpTransApplicationServiceTest {

    private AutenticacaoPort autenticacaoPort;
    private SpTransDadosPort spTransDadosPort;
    private SpTransApplicationService service;

    @BeforeEach
    void setUp() {
        autenticacaoPort = mock(AutenticacaoPort.class);
        spTransDadosPort = mock(SpTransDadosPort.class);
        service = new SpTransApplicationService(autenticacaoPort, spTransDadosPort);
    }

    @Test
    void deveLancarIndiceLinhaInvalidoQuandoIndiceForaDoIntervalo() {
        when(autenticacaoPort.autenticar()).thenReturn("cookie");
        when(spTransDadosPort.buscarLinha("terminal", "cookie"))
                .thenReturn(List.of(new LinhaResponse(1, true, "8000", 1, 1, "Terminal", "Term.")));

        IndiceLinhaInvalidoException exception = assertThrows(IndiceLinhaInvalidoException.class,
                () -> service.buscarPorTermo("terminal", 0));

        assertEquals("Índice de linha informado é inválido.", exception.getMessage());
    }

    @Test
    void deveBuscarPosicaoDaLinhaNoIndiceInformado() {
        PosicaoBusResponse esperado = new PosicaoBusResponse();
        when(autenticacaoPort.autenticar()).thenReturn("cookie");
        when(spTransDadosPort.buscarLinha("8000", "cookie")).thenReturn(List.of(
                new LinhaResponse(10, false, "8000", 1, 10, "Lapa", "Centro"),
                new LinhaResponse(20, false, "8000", 2, 10, "Lapa", "Centro")));
        when(spTransDadosPort.buscarPosicaoLinha(20, "cookie")).thenReturn(esperado);

        assertSame(esperado, service.buscarPorTermo("8000", 2));
    }

    @Test
    void deveRetornarListaVaziaQuandoSpTransNaoRetornaLinhas() {
        when(autenticacaoPort.autenticar()).thenReturn("cookie");
        when(spTransDadosPort.buscarLinha("xyz", "cookie")).thenReturn(null);

        assertTrue(service.buscarLinhas("xyz").isEmpty());
    }

    @Test
    void deveRenovarSessaoETentarNovamenteQuandoSessaoExpira() {
        PosicaoBusResponse esperado = new PosicaoBusResponse();
        when(autenticacaoPort.autenticar()).thenReturn("antigo", "novo");
        when(spTransDadosPort.buscarPosicaoLinha(123, "antigo")).thenThrow(new SessaoExpiradaException());
        when(spTransDadosPort.buscarPosicaoLinha(123, "novo")).thenReturn(esperado);

        assertSame(esperado, service.buscarPorCodigo(123));
        verify(autenticacaoPort).invalidarSessao();
    }

    @Test
    void deveDesistirQuandoSessaoExpiraNovamenteAposRenovar() {
        when(autenticacaoPort.autenticar()).thenReturn("antigo", "novo");
        when(spTransDadosPort.buscarPosicaoLinha(123, "antigo")).thenThrow(new SessaoExpiradaException());
        when(spTransDadosPort.buscarPosicaoLinha(123, "novo")).thenThrow(new SessaoExpiradaException());

        assertThrows(SessaoExpiradaException.class, () -> service.buscarPorCodigo(123));
        verify(autenticacaoPort, times(2)).autenticar();
    }
}
