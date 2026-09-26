package io.github.matheuslimajv.radarsptrans.infrastructure.adapter.in.rest;

import io.github.matheuslimajv.radarsptrans.domain.model.Itinerario;
import io.github.matheuslimajv.radarsptrans.domain.model.Linha;
import io.github.matheuslimajv.radarsptrans.domain.model.ParadaProxima;
import io.github.matheuslimajv.radarsptrans.domain.model.PosicaoLinha;
import io.github.matheuslimajv.radarsptrans.domain.model.TempoEsperaParada;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarItinerarioUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarLinhasUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarParadasProximasUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarPosicaoPorCodigoUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarPosicaoPorTermoUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.CalcularTempoEsperaUseCase;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sptrans")
public class SpTransController {

    private final BuscarPosicaoPorTermoUseCase buscarPosicaoPorTermoUseCase;
    private final BuscarPosicaoPorCodigoUseCase buscarPosicaoPorCodigoUseCase;
    private final BuscarLinhasUseCase buscarLinhasUseCase;
    private final BuscarParadasProximasUseCase buscarParadasProximasUseCase;
    private final CalcularTempoEsperaUseCase calcularTempoEsperaUseCase;
    private final BuscarItinerarioUseCase buscarItinerarioUseCase;

    public SpTransController(BuscarPosicaoPorTermoUseCase buscarPosicaoPorTermoUseCase,
                             BuscarPosicaoPorCodigoUseCase buscarPosicaoPorCodigoUseCase,
                             BuscarLinhasUseCase buscarLinhasUseCase,
                             BuscarParadasProximasUseCase buscarParadasProximasUseCase,
                             CalcularTempoEsperaUseCase calcularTempoEsperaUseCase,
                             BuscarItinerarioUseCase buscarItinerarioUseCase) {
        this.buscarPosicaoPorTermoUseCase = buscarPosicaoPorTermoUseCase;
        this.buscarPosicaoPorCodigoUseCase = buscarPosicaoPorCodigoUseCase;
        this.buscarLinhasUseCase = buscarLinhasUseCase;
        this.buscarParadasProximasUseCase = buscarParadasProximasUseCase;
        this.calcularTempoEsperaUseCase = calcularTempoEsperaUseCase;
        this.buscarItinerarioUseCase = buscarItinerarioUseCase;
    }

    @GetMapping("/linhas")
    public List<Linha> listarLinhas(@RequestParam("termosBusca") @NotBlank String termosBusca) {
        return buscarLinhasUseCase.buscarLinhas(termosBusca);
    }

    /**
     * @deprecated o índice depende da ordem em que a SPTrans devolve as linhas; use {@code /linhas} para
     * escolher a linha e {@code /posicao} com o código dela.
     */
    @Deprecated
    @GetMapping("/buscar")
    public ResponseEntity<PosicaoLinha> buscarLinhas(@RequestParam("termosBusca") @NotBlank String termosBusca,
                                                     @RequestParam("indice") @Min(1) int indice) {
        return ResponseEntity.ok()
                .header("Deprecation", "true")
                .body(buscarPosicaoPorTermoUseCase.buscarPorTermo(termosBusca, indice));
    }

    @GetMapping("/posicao")
    public PosicaoLinha localBus(@RequestParam("codigoLinha") @Positive int codigoLinha) {
        return buscarPosicaoPorCodigoUseCase.buscarPorCodigo(codigoLinha);
    }

    @GetMapping("/paradas/proximas")
    public List<ParadaProxima> paradasProximas(
            @RequestParam("latitude") @DecimalMin("-90") @DecimalMax("90") double latitude,
            @RequestParam("longitude") @DecimalMin("-180") @DecimalMax("180") double longitude,
            @RequestParam(value = "raio", defaultValue = "500") @Min(1) @Max(2000) int raioMetros,
            @RequestParam(value = "limite", defaultValue = "20") @Min(1) @Max(100) int limite) {
        return buscarParadasProximasUseCase.buscarParadasProximas(latitude, longitude, raioMetros, limite);
    }

    @GetMapping("/paradas/tempo-espera")
    public List<TempoEsperaParada> tempoEspera(
            @RequestParam("termosBusca") @NotBlank String termosBusca,
            @RequestParam("latitude") @DecimalMin("-90") @DecimalMax("90") double latitude,
            @RequestParam("longitude") @DecimalMin("-180") @DecimalMax("180") double longitude,
            @RequestParam(value = "codigoLinha", required = false) @Positive Integer codigoLinha) {
        return calcularTempoEsperaUseCase.calcularTempoEspera(termosBusca, codigoLinha, latitude, longitude);
    }

    /** Paradas da linha em ordem de passagem, com o tempo de percurso programado (GTFS). */
    @GetMapping("/itinerario")
    public Itinerario itinerario(@RequestParam("letreiro") @NotBlank String letreiro,
                                 @RequestParam("sentido") @Min(1) @Max(2) int sentido) {
        return buscarItinerarioUseCase.buscarItinerario(letreiro, sentido);
    }
}
