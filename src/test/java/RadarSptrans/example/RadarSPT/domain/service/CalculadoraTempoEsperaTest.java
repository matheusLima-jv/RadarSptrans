package RadarSptrans.example.RadarSPT.domain.service;

import RadarSptrans.example.RadarSPT.domain.model.ChegadaEstimada;
import RadarSptrans.example.RadarSPT.domain.model.ChegadaPrevista;
import RadarSptrans.example.RadarSPT.domain.model.Itinerario;
import RadarSptrans.example.RadarSPT.domain.model.Parada;
import RadarSptrans.example.RadarSPT.domain.model.ParadaItinerario;
import RadarSptrans.example.RadarSPT.domain.model.PosicaoLinha;
import RadarSptrans.example.RadarSPT.domain.model.PrevisaoParada;
import RadarSptrans.example.RadarSPT.domain.model.Veiculo;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculadoraTempoEsperaTest {

    private static final Instant AGORA = Instant.parse("2024-01-03T13:00:00Z");

    // Quatro paradas em linha reta, ~1,1 km entre elas (0,01° de latitude), 3 minutos de percurso cada.
    private final Itinerario itinerario = new Itinerario("8000-10", 1, List.of(
            parada(1, -23.50, 0), parada(2, -23.51, 180), parada(3, -23.52, 360), parada(4, -23.53, 540)),
            EnumSet.allOf(DayOfWeek.class), List.of());

    @Test
    void deveEncontrarParadaMaisProxima() {
        assertEquals(2, CalculadoraTempoEspera.indiceParadaMaisProxima(itinerario, -23.521, -46.60));
    }

    @Test
    void distanciaEntreParadasVizinhasEhDeCercaDeUmQuilometro() {
        double metros = CalculadoraTempoEspera.distanciaMetros(itinerario.paradas().get(0), -23.51, -46.60);

        assertEquals(1112, metros, 5);
    }

    @Test
    void deveConverterPrevisaoOficialEmMinutosOrdenados() {
        PrevisaoParada previsao = new PrevisaoParada("19:55", List.of(
                new ChegadaPrevista("B", "20:12", true), new ChegadaPrevista("A", "19:58", false)));

        List<ChegadaEstimada> chegadas = CalculadoraTempoEspera.chegadasPrevistas(previsao);

        assertEquals(List.of(new ChegadaEstimada("A", 3, "19:58", false), new ChegadaEstimada("B", 17, "20:12", true)),
                chegadas);
    }

    @Test
    void previsaoAposMeiaNoiteNaoViraNegativa() {
        PrevisaoParada previsao = new PrevisaoParada("23:58", List.of(new ChegadaPrevista("A", "00:05", true)));

        assertEquals(7, CalculadoraTempoEspera.chegadasPrevistas(previsao).get(0).minutos());
    }

    @Test
    void previsaoLigeiramenteAtrasadaViraZeroEmVezDeUmDiaInteiro() {
        PrevisaoParada previsao = new PrevisaoParada("10:05", List.of(new ChegadaPrevista("A", "10:04", true)));

        assertEquals(0, CalculadoraTempoEspera.chegadasPrevistas(previsao).get(0).minutos());
    }

    @Test
    void deveEstimarPelaPosicaoSomenteOnibusQueAindaNaoPassaram() {
        PosicaoLinha posicao = new PosicaoLinha("10:00", List.of(
                veiculo("antes", -23.5002, AGORA),     // perto da parada 1: 6 min até a parada 3
                veiculo("na-parada", -23.5201, AGORA), // já na parada 3: 0 min
                veiculo("depois", -23.5299, AGORA)));  // na parada 4: já passou

        List<ChegadaEstimada> chegadas = CalculadoraTempoEspera.chegadasPorPosicao(itinerario, 2, posicao, AGORA);

        assertEquals(List.of(new ChegadaEstimada("na-parada", 0, "10:00", true),
                new ChegadaEstimada("antes", 6, "10:06", true)), chegadas);
    }

    @Test
    void deveIgnorarOnibusForaDaRotaOuComPosicaoAntiga() {
        PosicaoLinha posicao = new PosicaoLinha("10:00", List.of(
                new Veiculo("garagem", true, AGORA, -23.50, -46.70),                  // ~10 km a oeste da rota
                veiculo("parado-ha-10-min", -23.50, AGORA.minusSeconds(600))));

        assertTrue(CalculadoraTempoEspera.chegadasPorPosicao(itinerario, 2, posicao, AGORA).isEmpty());
    }

    @Test
    void intervaloMedioPrecisaDeDuasChegadas() {
        assertNull(CalculadoraTempoEspera.intervaloMedioMinutos(List.of(new ChegadaEstimada("A", 3, "10:03", true))));
        assertEquals(7.5, CalculadoraTempoEspera.intervaloMedioMinutos(List.of(
                new ChegadaEstimada("A", 3, "10:03", true),
                new ChegadaEstimada("B", 10, "10:10", true),
                new ChegadaEstimada("C", 18, "10:18", true))));
    }

    private static ParadaItinerario parada(long codigo, double latitude, int segundos) {
        return new ParadaItinerario(new Parada(codigo, "Parada " + codigo, null, latitude, -46.60), segundos);
    }

    private static Veiculo veiculo(String prefixo, double latitude, Instant atualizadoEm) {
        return new Veiculo(prefixo, true, atualizadoEm, latitude, -46.60);
    }
}
