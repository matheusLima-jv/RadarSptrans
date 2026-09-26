package RadarSptrans.example.RadarSPT.application.service;

import RadarSptrans.example.RadarSPT.domain.exception.DadosProgramadosIndisponiveisException;
import RadarSptrans.example.RadarSPT.domain.model.ChegadaPrevista;
import RadarSptrans.example.RadarSPT.domain.model.FonteChegadas;
import RadarSptrans.example.RadarSPT.domain.model.Itinerario;
import RadarSptrans.example.RadarSPT.domain.model.JanelaFrequencia;
import RadarSptrans.example.RadarSPT.domain.model.Linha;
import RadarSptrans.example.RadarSPT.domain.model.Parada;
import RadarSptrans.example.RadarSPT.domain.model.ParadaItinerario;
import RadarSptrans.example.RadarSPT.domain.model.PosicaoLinha;
import RadarSptrans.example.RadarSPT.domain.model.PrevisaoParada;
import RadarSptrans.example.RadarSPT.domain.model.TempoEsperaParada;
import RadarSptrans.example.RadarSPT.domain.model.Veiculo;
import RadarSptrans.example.RadarSPT.domain.port.in.BuscarLinhasUseCase;
import RadarSptrans.example.RadarSPT.domain.port.out.AutenticacaoPort;
import RadarSptrans.example.RadarSPT.domain.port.out.DadosProgramadosPort;
import RadarSptrans.example.RadarSPT.domain.port.out.SpTransDadosPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParadaApplicationServiceTest {

    // Quarta-feira, 07:30 em São Paulo.
    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2024-01-03T10:30:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final Linha LINHA = new Linha(504, "7545-10", 1, "JD. JOÃO XXIII", "PÇA. RAMOS DE AZEVEDO", false);
    private static final Parada PARADA_1 = new Parada(1, "Parada 1", null, -23.50, -46.60);
    private static final Parada PARADA_2 = new Parada(2, "Parada 2", null, -23.51, -46.60);
    private static final Itinerario ITINERARIO = new Itinerario("7545-10", 1,
            List.of(new ParadaItinerario(PARADA_1, 0), new ParadaItinerario(PARADA_2, 300)),
            EnumSet.allOf(DayOfWeek.class), List.of(new JanelaFrequencia(7 * 3600, 7 * 3600 + 59 * 60, 900)));

    private BuscarLinhasUseCase buscarLinhas;
    private DadosProgramadosPort dadosProgramados;
    private SpTransDadosPort spTransDados;
    private ParadaApplicationService service;

    @BeforeEach
    void setUp() {
        buscarLinhas = mock(BuscarLinhasUseCase.class);
        dadosProgramados = mock(DadosProgramadosPort.class);
        spTransDados = mock(SpTransDadosPort.class);
        AutenticacaoPort autenticacao = mock(AutenticacaoPort.class);
        when(autenticacao.autenticar()).thenReturn("cookie");
        when(dadosProgramados.disponivel()).thenReturn(true);
        service = new ParadaApplicationService(buscarLinhas, dadosProgramados, spTransDados,
                new SessaoSpTrans(autenticacao), RELOGIO);
    }

    @Test
    void deveUsarPrevisaoOficialQuandoAParadaTemPrevisao() {
        when(buscarLinhas.buscarLinhas("7545")).thenReturn(List.of(LINHA));
        when(dadosProgramados.buscarItinerario("7545-10", 1)).thenReturn(Optional.of(ITINERARIO));
        when(spTransDados.buscarParadasComPrevisao(504, "cookie")).thenReturn(List.of(PARADA_2));
        when(spTransDados.buscarPrevisao(2, 504, "cookie")).thenReturn(new PrevisaoParada("07:30", List.of(
                new ChegadaPrevista("A", "07:34", true), new ChegadaPrevista("B", "07:50", true))));

        TempoEsperaParada resultado = service.calcularTempoEspera("7545", null, -23.5101, -46.60).get(0);

        assertEquals(PARADA_2, resultado.parada());
        assertEquals(11, resultado.distanciaMetros());
        assertEquals(15.0, resultado.intervaloProgramadoMinutos());
        assertEquals(7.5, resultado.esperaMediaMinutos());
        assertEquals(FonteChegadas.PREVISAO_SPTRANS, resultado.fonteChegadas());
        assertEquals(4, resultado.proximaChegadaMinutos());
        assertEquals(16.0, resultado.intervaloObservadoMinutos());
        verify(spTransDados, never()).buscarPosicaoLinha(anyInt(), anyString());
    }

    @Test
    void deveEstimarPelaPosicaoQuandoAParadaNaoTemPrevisaoOficial() {
        when(buscarLinhas.buscarLinhas("7545")).thenReturn(List.of(LINHA));
        when(dadosProgramados.buscarItinerario("7545-10", 1)).thenReturn(Optional.of(ITINERARIO));
        when(spTransDados.buscarParadasComPrevisao(504, "cookie")).thenReturn(List.of());
        when(spTransDados.buscarPosicaoLinha(504, "cookie")).thenReturn(new PosicaoLinha("07:30",
                List.of(new Veiculo("A", true, RELOGIO.instant(), -23.5001, -46.60))));

        TempoEsperaParada resultado = service.calcularTempoEspera("7545", null, -23.5101, -46.60).get(0);

        assertEquals(FonteChegadas.POSICAO_VEICULOS, resultado.fonteChegadas());
        assertEquals(5, resultado.proximaChegadaMinutos());
        verify(spTransDados, never()).buscarPrevisao(anyLong(), anyInt(), anyString());
    }

    @Test
    void semOnibusACaminhoInformaSemDadosMasMantemIntervaloProgramado() {
        when(buscarLinhas.buscarLinhas("7545")).thenReturn(List.of(LINHA));
        when(dadosProgramados.buscarItinerario("7545-10", 1)).thenReturn(Optional.of(ITINERARIO));
        when(spTransDados.buscarParadasComPrevisao(504, "cookie")).thenReturn(List.of(PARADA_2));
        when(spTransDados.buscarPrevisao(2, 504, "cookie")).thenReturn(PrevisaoParada.vazia("07:30"));
        when(spTransDados.buscarPosicaoLinha(504, "cookie")).thenReturn(new PosicaoLinha("07:30", List.of()));

        TempoEsperaParada resultado = service.calcularTempoEspera("7545", null, -23.5101, -46.60).get(0);

        assertEquals(FonteChegadas.SEM_DADOS, resultado.fonteChegadas());
        assertNull(resultado.proximaChegadaMinutos());
        assertEquals(15.0, resultado.intervaloProgramadoMinutos());
    }

    @Test
    void deveFiltrarPeloCodigoDaLinhaEIgnorarLinhasSemItinerario() {
        Linha outroSentido = new Linha(33272, "7545-10", 2, "PÇA. RAMOS DE AZEVEDO", "JD. JOÃO XXIII", false);
        Linha semGtfs = new Linha(999, "9999-10", 1, "A", "B", false);
        when(buscarLinhas.buscarLinhas("7545")).thenReturn(List.of(LINHA, outroSentido, semGtfs));
        when(dadosProgramados.buscarItinerario("7545-10", 1)).thenReturn(Optional.of(ITINERARIO));
        when(dadosProgramados.buscarItinerario("9999-10", 1)).thenReturn(Optional.empty());
        when(spTransDados.buscarParadasComPrevisao(anyInt(), anyString())).thenReturn(List.of());
        when(spTransDados.buscarPosicaoLinha(anyInt(), anyString())).thenReturn(new PosicaoLinha("07:30", List.of()));

        assertEquals(List.of(504), service.calcularTempoEspera("7545", 504, -23.5, -46.6).stream()
                .map(r -> r.linha().codigo()).toList());
        assertEquals(List.of(504), service.calcularTempoEspera("7545", null, -23.5, -46.6).stream()
                .map(r -> r.linha().codigo()).toList());
    }

    @Test
    void deveLimitarAsLinhasMaisProximasParaNaoConsultarATodas() {
        List<Linha> linhas = IntStream.range(0, 10)
                .mapToObj(i -> new Linha(i + 1, "L" + i + "-10", 1, "A", "B", false)).toList();
        when(buscarLinhas.buscarLinhas("Lapa")).thenReturn(linhas);
        for (int i = 0; i < 10; i++) {
            Parada parada = new Parada(100 + i, "P" + i, null, -23.50 - i * 0.001, -46.60);
            when(dadosProgramados.buscarItinerario("L" + i + "-10", 1)).thenReturn(Optional.of(new Itinerario(
                    "L" + i + "-10", 1, List.of(new ParadaItinerario(parada, 0)), EnumSet.allOf(DayOfWeek.class),
                    List.of())));
        }
        when(spTransDados.buscarParadasComPrevisao(anyInt(), anyString())).thenReturn(List.of());
        when(spTransDados.buscarPosicaoLinha(anyInt(), anyString())).thenReturn(new PosicaoLinha("07:30", List.of()));

        List<TempoEsperaParada> resultado = service.calcularTempoEspera("Lapa", null, -23.50, -46.60);

        assertEquals(ParadaApplicationService.MAXIMO_LINHAS_TEMPO_ESPERA, resultado.size());
        assertEquals(List.of(1, 2, 3, 4), resultado.stream().map(r -> r.linha().codigo()).toList());
    }

    @Test
    void deveFalharQuandoGtfsAindaNaoCarregou() {
        when(dadosProgramados.disponivel()).thenReturn(false);

        assertThrows(DadosProgramadosIndisponiveisException.class,
                () -> service.calcularTempoEspera("7545", null, -23.5, -46.6));
        assertThrows(DadosProgramadosIndisponiveisException.class,
                () -> service.buscarParadasProximas(-23.5, -46.6, 500, 10));
    }
}
