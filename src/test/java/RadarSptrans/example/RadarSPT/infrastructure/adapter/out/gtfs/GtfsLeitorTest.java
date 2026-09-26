package RadarSptrans.example.RadarSPT.infrastructure.adapter.out.gtfs;

import RadarSptrans.example.RadarSPT.domain.model.Itinerario;
import RadarSptrans.example.RadarSPT.domain.model.JanelaFrequencia;
import RadarSptrans.example.RadarSPT.domain.model.ParadaItinerario;
import RadarSptrans.example.RadarSPT.domain.model.ParadaProxima;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtfsLeitorTest {

    // Trechos no formato real do GTFS da SPTrans (aspas, BOM, endereço com vírgula, stop_times fora de ordem).
    private static final Map<String, String> GTFS = new LinkedHashMap<>(Map.of(
            "stops.txt", "﻿stop_id,\"stop_name\",\"stop_desc\",stop_lat,stop_lon\n"
                    + "260015039,\"Paulista B/C\",\"R. Da Consolação, 2483 Ref.: Av Paulista\",-23.555883,-46.66306\n"
                    + "260016855,\"Paulista C/B\",\"\",-23.555176,-46.66237\n"
                    + "7014417,\"Angelica B/C\",\"\",-23.534564,-46.654302\n",
            "trips.txt", "\"route_id\",\"service_id\",\"trip_id\",\"trip_headsign\",\"direction_id\",\"shape_id\"\n"
                    + "\"7545-10\",\"U__\",\"7545-10-0\",\"Pça. Ramos De Azevedo\",\"0\",\"1\"\n"
                    + "\"7545-10\",\"U__\",\"7545-10-1\",\"Jd. João Xxiii\",\"1\",\"2\"\n",
            "stop_times.txt", "\"trip_id\",\"arrival_time\",\"departure_time\",\"stop_id\",\"stop_sequence\"\n"
                    + "\"7545-10-0\",\"07:04:00\",\"07:04:00\",\"7014417\",\"2\"\n"
                    + "\"7545-10-0\",\"07:00:00\",\"07:00:00\",\"260015039\",\"1\"\n"
                    + "\"7545-10-1\",\"07:00:00\",\"07:00:00\",\"260016855\",\"1\"\n",
            "frequencies.txt", "\"trip_id\",\"start_time\",\"end_time\",\"headway_secs\"\n"
                    + "\"7545-10-0\",\"07:00:00\",\"07:59:00\",\"600\"\n",
            "calendar.txt", "\"service_id\",\"monday\",\"tuesday\",\"wednesday\",\"thursday\",\"friday\",\"saturday\","
                    + "\"sunday\",\"start_date\",\"end_date\"\n"
                    + "\"U__\",\"1\",\"1\",\"1\",\"1\",\"1\",\"0\",\"0\",\"20231001\",\"20270401\"\n"));

    @Test
    void deveMontarItinerarioNaOrdemDePassagemComTempoDePercurso() throws IOException {
        GtfsDados dados = GtfsLeitor.ler(zip(GTFS));

        Itinerario itinerario = dados.itinerario("7545-10", 1).orElseThrow();

        assertEquals(List.of(260015039L, 7014417L),
                itinerario.paradas().stream().map(p -> p.parada().codigo()).toList());
        assertEquals(List.of(0, 240), itinerario.paradas().stream().map(ParadaItinerario::segundosDesdeInicio).toList());
        assertEquals(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), itinerario.diasOperacao());
        assertEquals(List.of(new JanelaFrequencia(7 * 3600, 7 * 3600 + 59 * 60, 600)), itinerario.frequencias());
        assertEquals("R. Da Consolação, 2483 Ref.: Av Paulista", itinerario.paradas().get(0).parada().endereco());
    }

    @Test
    void sentidoDoisCorrespondeADirecaoUmDoGtfs() throws IOException {
        GtfsDados dados = GtfsLeitor.ler(zip(GTFS));

        assertEquals(260016855L, dados.itinerario("7545-10", 2).orElseThrow().paradas().get(0).parada().codigo());
        assertTrue(dados.itinerario("7545-10", 3).isEmpty());
    }

    @Test
    void deveListarParadasProximasComAsLinhasQuePassamNelas() throws IOException {
        GtfsDados dados = GtfsLeitor.ler(zip(GTFS));

        List<ParadaProxima> proximas = dados.paradasProximas(-23.5560, -46.6630, 200, 10);

        assertEquals(List.of(260015039L, 260016855L), proximas.stream().map(p -> p.parada().codigo()).toList());
        assertEquals(List.of("7545-10"), proximas.get(0).linhas());
        assertTrue(proximas.get(0).distanciaMetros() < proximas.get(1).distanciaMetros());
    }

    @Test
    void deveFalharQuandoFaltaArquivoObrigatorio() {
        Map<String, String> incompleto = new LinkedHashMap<>(GTFS);
        incompleto.remove("frequencies.txt");

        assertThrows(IOException.class, () -> GtfsLeitor.ler(zip(incompleto)));
    }

    @Test
    void deveConverterHorarioGtfsEmSegundos() {
        assertEquals(25 * 3600 + 90, GtfsLeitor.segundos("25:01:30"));
    }

    private static ByteArrayInputStream zip(Map<String, String> arquivos) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("shapes.txt"));
            zip.write("shape_id,shape_pt_lat,shape_pt_lon,shape_pt_sequence\n".getBytes(StandardCharsets.UTF_8));
            for (Map.Entry<String, String> arquivo : arquivos.entrySet()) {
                zip.putNextEntry(new ZipEntry(arquivo.getKey()));
                zip.write(arquivo.getValue().getBytes(StandardCharsets.UTF_8));
            }
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }
}
