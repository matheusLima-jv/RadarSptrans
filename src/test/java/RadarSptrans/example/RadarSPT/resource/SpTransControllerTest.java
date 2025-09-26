package RadarSptrans.example.RadarSPT.resource;

import RadarSptrans.example.RadarSPT.domain.exception.AutenticacaoException;
import RadarSptrans.example.RadarSPT.domain.exception.IndiceLinhaInvalidoException;
import RadarSptrans.example.RadarSPT.domain.model.PosicaoBus;
import RadarSptrans.example.RadarSPT.domain.model.PosicaoBusResponse;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarPosicaoPorCodigoUseCase;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarPosicaoPorTermoUseCase;
import RadarSptrans.example.RadarSPT.infrastructure.adapter.in.rest.SpTransController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpTransControllerTest {

    @Mock
    private BuscarPosicaoPorTermoUseCase buscarPosicaoPorTermoUseCase;

    @Mock
    private BuscarPosicaoPorCodigoUseCase buscarPosicaoPorCodigoUseCase;

    @InjectMocks
    private SpTransController controller;

    @Test
    void buscarLinhasRetornaRespostaDoCasoDeUso() {
        PosicaoBusResponse expectedResponse = new PosicaoBusResponse(
                "10:00",
                List.of(new PosicaoBus("1234", true, null, -23.0, -46.0, null, null))
        );
        when(buscarPosicaoPorTermoUseCase.buscarPorTermo("term", 1)).thenReturn(expectedResponse);

        PosicaoBusResponse resultado = controller.buscarLinhas("term", 1);

        assertEquals(expectedResponse, resultado);
        verify(buscarPosicaoPorTermoUseCase).buscarPorTermo("term", 1);
    }

    @Test
    void buscarLinhasPropagaExcecoesDoCasoDeUso() {
        when(buscarPosicaoPorTermoUseCase.buscarPorTermo("term", 2)).thenThrow(new IndiceLinhaInvalidoException());

        assertThrows(IndiceLinhaInvalidoException.class, () -> controller.buscarLinhas("term", 2));
    }

    @Test
    void localBusRetornaRespostaDoCasoDeUso() {
        PosicaoBusResponse expectedResponse = new PosicaoBusResponse(
                "11:00",
                List.of(new PosicaoBus("4321", false, null, -22.0, -45.0, null, null))
        );
        when(buscarPosicaoPorCodigoUseCase.buscarPorCodigo("123")).thenReturn(expectedResponse);

        PosicaoBusResponse resultado = controller.localBus("123");

        assertEquals(expectedResponse, resultado);
        verify(buscarPosicaoPorCodigoUseCase).buscarPorCodigo("123");
    }

    @Test
    void localBusPropagaExcecoesDoCasoDeUso() {
        when(buscarPosicaoPorCodigoUseCase.buscarPorCodigo("123")).thenThrow(new AutenticacaoException());

        assertThrows(AutenticacaoException.class, () -> controller.localBus("123"));
    }
}
