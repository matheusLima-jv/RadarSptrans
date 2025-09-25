package RadarSptrans.example.RadarSPT.application.service;

import RadarSptrans.example.RadarSPT.domain.exception.IndiceLinhaInvalidoException;
import RadarSptrans.example.RadarSPT.domain.model.LinhaResponse;
import RadarSptrans.example.RadarSPT.domain.port.out.AutenticacaoPort;
import RadarSptrans.example.RadarSPT.domain.port.out.SpTransDadosPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
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
}
