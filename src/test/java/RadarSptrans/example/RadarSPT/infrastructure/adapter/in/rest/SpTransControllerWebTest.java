package RadarSptrans.example.RadarSPT.infrastructure.adapter.in.rest;

import RadarSptrans.example.RadarSPT.domain.exception.SpTransIndisponivelException;
import RadarSptrans.example.RadarSPT.domain.model.LinhaResponse;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarLinhasUseCase;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarPosicaoPorCodigoUseCase;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarPosicaoPorTermoUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SpTransController.class)
class SpTransControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BuscarPosicaoPorTermoUseCase buscarPosicaoPorTermoUseCase;

    @MockBean
    private BuscarPosicaoPorCodigoUseCase buscarPosicaoPorCodigoUseCase;

    @MockBean
    private BuscarLinhasUseCase buscarLinhasUseCase;

    @Test
    void deveListarLinhas() throws Exception {
        when(buscarLinhasUseCase.buscarLinhas("8000"))
                .thenReturn(List.of(new LinhaResponse(33887, false, "8000", 1, 10, "Lapa", "Centro")));

        mockMvc.perform(get("/api/sptrans/linhas").param("termosBusca", "8000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cl").value(33887));
    }

    @Test
    void deveRetornar400QuandoParametroObrigatorioAusente() throws Exception {
        mockMvc.perform(get("/api/sptrans/buscar").param("indice", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAMETRO_INVALIDO"))
                .andExpect(jsonPath("$.message").value("Parâmetro obrigatório ausente: termosBusca."));
    }

    @Test
    void deveRetornar400QuandoCodigoLinhaNaoNumerico() throws Exception {
        mockMvc.perform(get("/api/sptrans/posicao").param("codigoLinha", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAMETRO_INVALIDO"));

        verifyNoInteractions(buscarPosicaoPorCodigoUseCase);
    }

    @Test
    void deveRetornar400QuandoIndiceMenorQueUm() throws Exception {
        mockMvc.perform(get("/api/sptrans/buscar").param("termosBusca", "8000").param("indice", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAMETRO_INVALIDO"));

        verifyNoInteractions(buscarPosicaoPorTermoUseCase);
    }

    @Test
    void deveRetornar502QuandoSpTransIndisponivel() throws Exception {
        when(buscarPosicaoPorCodigoUseCase.buscarPorCodigo(123))
                .thenThrow(new SpTransIndisponivelException("Falha ao consultar a SPTrans (HTTP 503).", null));

        mockMvc.perform(get("/api/sptrans/posicao").param("codigoLinha", "123"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("SPTRANS_INDISPONIVEL"));
    }
}
