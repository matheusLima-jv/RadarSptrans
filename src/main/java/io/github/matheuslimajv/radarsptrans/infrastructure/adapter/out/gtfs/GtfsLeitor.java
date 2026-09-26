package io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.gtfs;

import io.github.matheuslimajv.radarsptrans.domain.model.Itinerario;
import io.github.matheuslimajv.radarsptrans.domain.model.JanelaFrequencia;
import io.github.matheuslimajv.radarsptrans.domain.model.Parada;
import io.github.matheuslimajv.radarsptrans.domain.model.ParadaItinerario;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Lê o zip GTFS da SPTrans. Convenções verificadas contra a API Olho Vivo:
 * route_id = letreiro ("8000-10"), direction_id = sentido - 1 e stop_id = código da parada (cp).
 */
final class GtfsLeitor {

    private static final Set<String> ARQUIVOS =
            Set.of("stops.txt", "trips.txt", "stop_times.txt", "frequencies.txt", "calendar.txt");
    private static final CSVFormat CSV = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreSurroundingSpaces(true)
            .build();
    private static final List<DayOfWeek> COLUNAS_DIAS = List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

    private GtfsLeitor() {
    }

    static GtfsDados ler(InputStream zip) throws IOException {
        Map<String, byte[]> arquivos = extrair(zip);
        for (String arquivo : ARQUIVOS) {
            if (!arquivos.containsKey(arquivo)) {
                throw new IOException("GTFS sem o arquivo " + arquivo);
            }
        }

        Map<Long, Parada> paradas = new HashMap<>();
        ler(arquivos.get("stops.txt"), r -> {
            long codigo = Long.parseLong(r.get("stop_id"));
            paradas.put(codigo, new Parada(codigo, r.get("stop_name"), vazioComoNulo(r.get("stop_desc")),
                    Double.parseDouble(r.get("stop_lat")), Double.parseDouble(r.get("stop_lon"))));
        });

        Map<String, Set<DayOfWeek>> diasPorServico = new HashMap<>();
        ler(arquivos.get("calendar.txt"), r -> {
            Set<DayOfWeek> dias = EnumSet.noneOf(DayOfWeek.class);
            for (DayOfWeek dia : COLUNAS_DIAS) {
                if ("1".equals(r.get(dia.name().toLowerCase()))) {
                    dias.add(dia);
                }
            }
            diasPorServico.put(r.get("service_id"), dias);
        });

        Map<String, List<JanelaFrequencia>> frequenciasPorViagem = new HashMap<>();
        ler(arquivos.get("frequencies.txt"), r -> frequenciasPorViagem
                .computeIfAbsent(r.get("trip_id"), k -> new ArrayList<>())
                .add(new JanelaFrequencia(segundos(r.get("start_time")), segundos(r.get("end_time")),
                        Integer.parseInt(r.get("headway_secs")))));

        Map<String, List<CSVRecord>> horariosPorViagem = new HashMap<>();
        ler(arquivos.get("stop_times.txt"), r ->
                horariosPorViagem.computeIfAbsent(r.get("trip_id"), k -> new ArrayList<>()).add(r));

        Map<String, Itinerario> itinerarios = new HashMap<>();
        ler(arquivos.get("trips.txt"), r -> {
            String viagem = r.get("trip_id");
            String letreiro = r.get("route_id");
            int sentido = Integer.parseInt(r.get("direction_id")) + 1;
            List<ParadaItinerario> sequencia = sequencia(horariosPorViagem.getOrDefault(viagem, List.of()), paradas);
            itinerarios.put(GtfsDados.chave(letreiro, sentido), new Itinerario(letreiro, sentido, sequencia,
                    diasPorServico.getOrDefault(r.get("service_id"), Set.of()),
                    frequenciasPorViagem.getOrDefault(viagem, List.of())));
        });

        return new GtfsDados(paradas, itinerarios);
    }

    private static List<ParadaItinerario> sequencia(List<CSVRecord> horarios, Map<Long, Parada> paradas) {
        List<CSVRecord> ordenados = horarios.stream()
                .sorted(Comparator.comparingInt(r -> Integer.parseInt(r.get("stop_sequence"))))
                .toList();
        if (ordenados.isEmpty()) {
            return List.of();
        }
        int inicio = segundos(ordenados.get(0).get("arrival_time"));
        List<ParadaItinerario> sequencia = new ArrayList<>();
        for (CSVRecord horario : ordenados) {
            Parada parada = paradas.get(Long.parseLong(horario.get("stop_id")));
            if (parada != null) {
                sequencia.add(new ParadaItinerario(parada, segundos(horario.get("arrival_time")) - inicio));
            }
        }
        return List.copyOf(sequencia);
    }

    private static Map<String, byte[]> extrair(InputStream zip) throws IOException {
        Map<String, byte[]> arquivos = new HashMap<>();
        try (ZipInputStream entrada = new ZipInputStream(zip, StandardCharsets.UTF_8)) {
            ZipEntry item;
            while ((item = entrada.getNextEntry()) != null) {
                String nome = item.getName().substring(item.getName().lastIndexOf('/') + 1);
                if (ARQUIVOS.contains(nome)) {
                    arquivos.put(nome, entrada.readAllBytes());
                }
            }
        }
        return arquivos;
    }

    private static void ler(byte[] conteudo, Consumer<CSVRecord> consumidor) throws IOException {
        try (Reader leitor = semBom(new InputStreamReader(new ByteArrayInputStream(conteudo), StandardCharsets.UTF_8));
             CSVParser parser = CSV.parse(leitor)) {
            for (CSVRecord registro : parser) {
                consumidor.accept(registro);
            }
        }
    }

    private static Reader semBom(Reader leitor) throws IOException {
        PushbackReader pushback = new PushbackReader(leitor, 1);
        int primeiro = pushback.read();
        if (primeiro != -1 && primeiro != '﻿') {
            pushback.unread(primeiro);
        }
        return pushback;
    }

    // "HH:MM:SS"; no GTFS a hora pode passar de 24 (viagens após a meia-noite).
    static int segundos(String horario) {
        String[] partes = horario.trim().split(":");
        return Integer.parseInt(partes[0]) * 3600 + Integer.parseInt(partes[1]) * 60 + Integer.parseInt(partes[2]);
    }

    private static String vazioComoNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }
}
