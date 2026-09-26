package RadarSptrans.example.RadarSPT.domain.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItinerarioTest {

    private static final int HORA = 3600;

    // Formato real do GTFS da SPTrans: janelas de hora cheia terminando em :59:00.
    private final Itinerario diasUteis = new Itinerario("7545-21", 1, List.of(),
            EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            List.of(new JanelaFrequencia(7 * HORA, 7 * HORA + 59 * 60, 600),
                    new JanelaFrequencia(8 * HORA, 8 * HORA + 59 * 60, 900)));

    @Test
    void deveRetornarIntervaloDaJanelaDoHorario() {
        // 2024-01-03 é quarta-feira
        assertEquals(Optional.of(600), diasUteis.intervaloProgramadoSegundos(LocalDateTime.of(2024, 1, 3, 7, 30)));
        assertEquals(Optional.of(900), diasUteis.intervaloProgramadoSegundos(LocalDateTime.of(2024, 1, 3, 8, 0)));
    }

    @Test
    void fimDaJanelaValeAteOUltimoSegundoDoMinuto() {
        assertEquals(Optional.of(600), diasUteis.intervaloProgramadoSegundos(LocalDateTime.of(2024, 1, 3, 7, 59, 30)));
    }

    @Test
    void deveRetornarVazioForaDoHorarioDeOperacao() {
        assertEquals(Optional.empty(), diasUteis.intervaloProgramadoSegundos(LocalDateTime.of(2024, 1, 3, 23, 0)));
    }

    @Test
    void deveRetornarVazioEmDiaSemOperacao() {
        // 2024-01-06 é sábado
        assertEquals(Optional.empty(), diasUteis.intervaloProgramadoSegundos(LocalDateTime.of(2024, 1, 6, 7, 30)));
    }

    @Test
    void janelaQuePassaDaMeiaNoiteValeNaMadrugada() {
        JanelaFrequencia madrugada = new JanelaFrequencia(23 * HORA, 25 * HORA, 1800);

        assertEquals(true, madrugada.contem(30 * 60));
        assertEquals(false, madrugada.contem(2 * HORA));
    }
}
