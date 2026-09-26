package RadarSptrans.example.RadarSPT.application.service;

import RadarSptrans.example.RadarSPT.domain.exception.IndiceLinhaInvalidoException;
import RadarSptrans.example.RadarSPT.domain.model.Linha;
import RadarSptrans.example.RadarSPT.domain.model.PosicaoLinha;
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
import static org.mockito.Mockito.when;

class SpTransApplicationServiceTest {

    private SpTransDadosPort spTransDadosPort;
    private SpTransApplicationService service;

    @BeforeEach
    void setUp() {
        AutenticacaoPort autenticacaoPort = mock(AutenticacaoPort.class);
        when(autenticacaoPort.autenticar()).thenReturn("cookie");
        spTransDadosPort = mock(SpTransDadosPort.class);
        service = new SpTransApplicationService(new SessaoSpTrans(autenticacaoPort), spTransDadosPort);
    }

    @Test
    void deveLancarIndiceLinhaInvalidoQuandoIndiceForaDoIntervalo() {
        when(spTransDadosPort.buscarLinhas("terminal", "cookie"))
                .thenReturn(List.of(new Linha(1, "8000-10", 1, "Lapa", "Centro", false)));

        IndiceLinhaInvalidoException exception = assertThrows(IndiceLinhaInvalidoException.class,
                () -> service.buscarPorTermo("terminal", 2));

        assertEquals("Índice de linha informado é inválido.", exception.getMessage());
    }

    @Test
    void deveBuscarPosicaoDaLinhaNoIndiceInformado() {
        PosicaoLinha esperado = new PosicaoLinha("10:00", List.of());
        when(spTransDadosPort.buscarLinhas("8000", "cookie")).thenReturn(List.of(
                new Linha(10, "8000-10", 1, "Lapa", "Centro", false),
                new Linha(20, "8000-10", 2, "Centro", "Lapa", false)));
        when(spTransDadosPort.buscarPosicaoLinha(20, "cookie")).thenReturn(esperado);

        assertSame(esperado, service.buscarPorTermo("8000", 2));
    }

    @Test
    void deveRetornarListaVaziaQuandoSpTransNaoRetornaLinhas() {
        when(spTransDadosPort.buscarLinhas("xyz", "cookie")).thenReturn(null);

        assertTrue(service.buscarLinhas("xyz").isEmpty());
    }
}
