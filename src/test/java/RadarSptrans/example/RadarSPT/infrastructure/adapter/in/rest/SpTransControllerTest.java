package RadarSptrans.example.RadarSPT.infrastructure.adapter.in.rest;

import RadarSptrans.example.RadarSPT.domain.exception.AutenticacaoException;
import RadarSptrans.example.RadarSPT.domain.exception.IndiceLinhaInvalidoException;
import RadarSptrans.example.RadarSPT.domain.model.PosicaoLinha;
import RadarSptrans.example.RadarSPT.domain.model.Veiculo;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarLinhasUseCase;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarParadasProximasUseCase;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarPosicaoPorCodigoUseCase;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarPosicaoPorTermoUseCase;
import RadarSptrans.example.RadarSPT.domain.port.in.CalcularTempoEsperaUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpTransControllerTest {

    @Mock
    private BuscarPosicaoPorTermoUseCase buscarPosicaoPorTermoUseCase;

    @Mock
    private BuscarPosicaoPorCodigoUseCase buscarPosicaoPorCodigoUseCase;

    @Mock
    private BuscarLinhasUseCase buscarLinhasUseCase;

    @Mock
    private BuscarParadasProximasUseCase buscarParadasProximasUseCase;

    @Mock
    private CalcularTempoEsperaUseCase calcularTempoEsperaUseCase;

    @InjectMocks
    private SpTransController controller;

    @Test
    void buscarLinhasRetornaRespostaDoCasoDeUsoMarcadaComoDescontinuada() {
        PosicaoLinha esperado = new PosicaoLinha("10:00", List.of(new Veiculo("1234", true, null, -23.0, -46.0)));
        when(buscarPosicaoPorTermoUseCase.buscarPorTermo("term", 1)).thenReturn(esperado);

        ResponseEntity<PosicaoLinha> resposta = controller.buscarLinhas("term", 1);

        assertEquals(esperado, resposta.getBody());
        assertEquals("true", resposta.getHeaders().getFirst("Deprecation"));
    }

    @Test
    void buscarLinhasPropagaExcecoesDoCasoDeUso() {
        when(buscarPosicaoPorTermoUseCase.buscarPorTermo("term", 2)).thenThrow(new IndiceLinhaInvalidoException());

        assertThrows(IndiceLinhaInvalidoException.class, () -> controller.buscarLinhas("term", 2));
    }

    @Test
    void localBusRetornaRespostaDoCasoDeUso() {
        PosicaoLinha esperado = new PosicaoLinha("11:00", List.of());
        when(buscarPosicaoPorCodigoUseCase.buscarPorCodigo(123)).thenReturn(esperado);

        assertEquals(esperado, controller.localBus(123));
    }

    @Test
    void localBusPropagaExcecoesDoCasoDeUso() {
        when(buscarPosicaoPorCodigoUseCase.buscarPorCodigo(123)).thenThrow(new AutenticacaoException());

        assertThrows(AutenticacaoException.class, () -> controller.localBus(123));
    }
}
