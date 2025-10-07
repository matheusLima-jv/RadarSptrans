package RadarSptrans.example.RadarSPT.config;

import RadarSptrans.example.RadarSPT.domain.model.PosicaoBusResponse;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarPosicaoPorCodigoUseCase;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarPosicaoPorTermoUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebConfigCorsTest {

    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:5500",
            "http://127.0.0.1:5500"
    );

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BuscarPosicaoPorTermoUseCase buscarPosicaoPorTermoUseCase;

    @MockBean
    private BuscarPosicaoPorCodigoUseCase buscarPosicaoPorCodigoUseCase;

    @BeforeEach
    void setUpMocks() {
        when(buscarPosicaoPorTermoUseCase.buscarPorTermo(anyString(), anyInt()))
                .thenReturn(new PosicaoBusResponse());
        when(buscarPosicaoPorCodigoUseCase.buscarPorCodigo(anyString()))
                .thenReturn(new PosicaoBusResponse());
    }

    @Test
    void shouldAllowConfiguredOriginsOnPreflightRequests() throws Exception {
        for (String origin : ALLOWED_ORIGINS) {
            mockMvc.perform(options("/api/sptrans/buscar")
                            .header("Origin", origin)
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin));
        }
    }

    @Test
    void shouldReturnCorsHeaderForActualRequestsFromAllowedOrigins() throws Exception {
        for (String origin : ALLOWED_ORIGINS) {
            mockMvc.perform(get("/api/sptrans/buscar")
                            .header("Origin", origin)
                            .param("termosBusca", "8000")
                            .param("indice", "0")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin));
        }
    }
}
