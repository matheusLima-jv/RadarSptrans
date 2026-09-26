package io.github.matheuslimajv.radarsptrans.infrastructure.adapter.in.rest;

import io.github.matheuslimajv.radarsptrans.domain.exception.DadosProgramadosIndisponiveisException;
import io.github.matheuslimajv.radarsptrans.domain.exception.ItinerarioNaoEncontradoException;
import io.github.matheuslimajv.radarsptrans.domain.exception.SpTransIndisponivelException;
import io.github.matheuslimajv.radarsptrans.domain.model.FonteChegadas;
import io.github.matheuslimajv.radarsptrans.domain.model.Itinerario;
import io.github.matheuslimajv.radarsptrans.domain.model.ParadaItinerario;
import io.github.matheuslimajv.radarsptrans.domain.model.Linha;
import io.github.matheuslimajv.radarsptrans.domain.model.Parada;
import io.github.matheuslimajv.radarsptrans.domain.model.ParadaProxima;
import io.github.matheuslimajv.radarsptrans.domain.model.TempoEsperaParada;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarItinerarioUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarLinhasUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarParadasProximasUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarPosicaoPorCodigoUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarPosicaoPorTermoUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.CalcularTempoEsperaUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SpTransController.class)
class SpTransControllerWebTest {

    private static final Linha LINHA = new Linha(504, "7545-10", 1, "JD. JOÃO XXIII", "PÇA. RAMOS DE AZEVEDO", false);
    private static final Parada PARADA = new Parada(260015039L, "Paulista B/C", null, -23.555883, -46.66306);

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
    void deveListarLinhasComNomesDoDominio() throws Exception {
        when(buscarLinhasUseCase.buscarLinhas("7545")).thenReturn(List.of(LINHA));

        mockMvc.perform(get("/api/sptrans/linhas").param("termosBusca", "7545"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value(504))
                .andExpect(jsonPath("$[0].letreiro").value("7545-10"))
                .andExpect(jsonPath("$[0].destino").value("PÇA. RAMOS DE AZEVEDO"));
    }

    @Test
    void buscarPorIndiceSinalizaDescontinuacao() throws Exception {
        mockMvc.perform(get("/api/sptrans/buscar").param("termosBusca", "8000").param("indice", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Deprecation", "true"));
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

    @Test
    void deveListarParadasProximasComRaioPadrao() throws Exception {
        when(buscarParadasProximasUseCase.buscarParadasProximas(-23.5565, -46.6625, 500, 20))
                .thenReturn(List.of(new ParadaProxima(PARADA, 89, List.of("7545-10"))));

        mockMvc.perform(get("/api/sptrans/paradas/proximas")
                        .param("latitude", "-23.5565").param("longitude", "-46.6625"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].parada.codigo").value(260015039L))
                .andExpect(jsonPath("$[0].distanciaMetros").value(89))
                .andExpect(jsonPath("$[0].linhas[0]").value("7545-10"));
    }

    @Test
    void deveRejeitarCoordenadaInvalidaERaioAcimaDoLimite() throws Exception {
        mockMvc.perform(get("/api/sptrans/paradas/proximas").param("latitude", "-123").param("longitude", "-46.6"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/sptrans/paradas/proximas")
                        .param("latitude", "-23.5").param("longitude", "-46.6").param("raio", "50000"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(buscarParadasProximasUseCase);
    }

    @Test
    void deveCalcularTempoDeEspera() throws Exception {
        when(calcularTempoEsperaUseCase.calcularTempoEspera("7545", 504, -23.5565, -46.6625))
                .thenReturn(List.of(new TempoEsperaParada(LINHA, PARADA, 89, 15.0, 7.5, List.of(), null, null,
                        FonteChegadas.SEM_DADOS, "20:04")));

        mockMvc.perform(get("/api/sptrans/paradas/tempo-espera").param("termosBusca", "7545")
                        .param("codigoLinha", "504").param("latitude", "-23.5565").param("longitude", "-46.6625"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].intervaloProgramadoMinutos").value(15.0))
                .andExpect(jsonPath("$[0].esperaMediaMinutos").value(7.5))
                .andExpect(jsonPath("$[0].fonteChegadas").value("SEM_DADOS"));
    }

    @Test
    void deveRetornar503EnquantoGtfsNaoCarregou() throws Exception {
        when(calcularTempoEsperaUseCase.calcularTempoEspera("7545", null, -23.5, -46.6))
                .thenThrow(new DadosProgramadosIndisponiveisException());

        mockMvc.perform(get("/api/sptrans/paradas/tempo-espera").param("termosBusca", "7545")
                        .param("latitude", "-23.5").param("longitude", "-46.6"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DADOS_PROGRAMADOS_INDISPONIVEIS"));
    }

    @Test
    void deveRetornarItinerarioDaLinha() throws Exception {
        when(buscarItinerarioUseCase.buscarItinerario("7545-10", 1)).thenReturn(new Itinerario("7545-10", 1,
                List.of(new ParadaItinerario(PARADA, 0)), EnumSet.allOf(DayOfWeek.class), List.of()));

        mockMvc.perform(get("/api/sptrans/itinerario").param("letreiro", "7545-10").param("sentido", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paradas[0].parada.codigo").value(260015039L))
                .andExpect(jsonPath("$.paradas[0].segundosDesdeInicio").value(0));
    }

    @Test
    void deveRetornar404QuandoItinerarioNaoExiste() throws Exception {
        when(buscarItinerarioUseCase.buscarItinerario("9999-10", 2))
                .thenThrow(new ItinerarioNaoEncontradoException("9999-10", 2));

        mockMvc.perform(get("/api/sptrans/itinerario").param("letreiro", "9999-10").param("sentido", "2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ITINERARIO_NAO_ENCONTRADO"));
    }

    @Test
    void deveRejeitarSentidoInvalido() throws Exception {
        mockMvc.perform(get("/api/sptrans/itinerario").param("letreiro", "7545-10").param("sentido", "3"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(buscarItinerarioUseCase);
    }
}
