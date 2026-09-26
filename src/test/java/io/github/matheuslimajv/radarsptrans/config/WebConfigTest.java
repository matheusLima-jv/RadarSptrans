package io.github.matheuslimajv.radarsptrans.config;

import io.github.matheuslimajv.radarsptrans.domain.model.PosicaoLinha;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarItinerarioUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarLinhasUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarParadasProximasUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarPosicaoPorCodigoUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarPosicaoPorTermoUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.CalcularTempoEsperaUseCase;
import io.github.matheuslimajv.radarsptrans.infrastructure.adapter.in.rest.SpTransController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @MockBean
    private BuscarLinhasUseCase buscarLinhasUseCase;

    @MockBean
    private BuscarParadasProximasUseCase buscarParadasProximasUseCase;

    @MockBean
    private CalcularTempoEsperaUseCase calcularTempoEsperaUseCase;

    @MockBean
    private BuscarItinerarioUseCase buscarItinerarioUseCase;

    @Test
    void deveLiberarPreflightParaTodasAsOrigensConfiguradas() throws Exception {
        for (String origem : List.of("https://example.com", "https://another.com")) {
            mockMvc.perform(options("/api/sptrans/posicao")
                            .header(HttpHeaders.ORIGIN, origem)
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origem));
        }
    }

    @Test
    void deveIncluirCabecalhoCorsNaRequisicaoReal() throws Exception {
        when(buscarPosicaoPorCodigoUseCase.buscarPorCodigo(123)).thenReturn(new PosicaoLinha("10:00", List.of()));

        mockMvc.perform(get("/api/sptrans/posicao").param("codigoLinha", "123")
                        .header(HttpHeaders.ORIGIN, "https://example.com"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://example.com"));
    }

    @Test
    void deveRecusarOrigemNaoConfiguradaEMetodoDeEscrita() throws Exception {
        mockMvc.perform(options("/api/sptrans/posicao")
                        .header(HttpHeaders.ORIGIN, "https://evil.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
        mockMvc.perform(options("/api/sptrans/posicao")
                        .header(HttpHeaders.ORIGIN, "https://example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE"))
                .andExpect(status().isForbidden());
    }
}
