package RadarSptrans.example.RadarSPT.config;

import RadarSptrans.example.RadarSPT.domain.port.in.BuscarPosicaoPorCodigoUseCase;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarPosicaoPorTermoUseCase;
import RadarSptrans.example.RadarSPT.infrastructure.adapter.in.rest.SpTransController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SpTransController.class)
@Import(WebConfig.class)
@TestPropertySource(properties = "sptrans.cors.allowed-origins=https://example.com,https://another.com")
class WebConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BuscarPosicaoPorTermoUseCase buscarPosicaoPorTermoUseCase;

    @MockBean
    private BuscarPosicaoPorCodigoUseCase buscarPosicaoPorCodigoUseCase;

    @Test
    void shouldAllowCorsForAllConfiguredOrigins() throws Exception {
        mockMvc.perform(options("/api/sptrans/buscar")
                        .header(HttpHeaders.ORIGIN, "https://example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://example.com"));

        mockMvc.perform(options("/api/sptrans/buscar")
                        .header(HttpHeaders.ORIGIN, "https://another.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://another.com"));
    }
}
