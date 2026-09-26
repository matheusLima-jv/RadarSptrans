package io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.gtfs;

import io.github.matheuslimajv.radarsptrans.domain.model.Itinerario;
import io.github.matheuslimajv.radarsptrans.domain.model.ParadaProxima;
import io.github.matheuslimajv.radarsptrans.domain.port.out.DadosProgramadosPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Carrega o GTFS em segundo plano (a aplicação sobe sem esperar os ~14 MB) e o recarrega diariamente.
 * Enquanto não há dados, {@link #disponivel()} é false; se uma recarga falhar, os dados anteriores continuam valendo.
 */
@Component
public class GtfsAdapter implements DadosProgramadosPort {

    private static final Logger log = LoggerFactory.getLogger(GtfsAdapter.class);
    private static final int TIMEOUT_CONEXAO_MS = (int) Duration.ofSeconds(10).toMillis();
    private static final int TIMEOUT_LEITURA_MS = (int) Duration.ofMinutes(2).toMillis();

    private final ResourceLoader resourceLoader;
    private final String url;
    private final boolean habilitado;
    private final AtomicReference<GtfsDados> dados = new AtomicReference<>();

    public GtfsAdapter(ResourceLoader resourceLoader,
                       @Value("${sptrans.gtfs.url}") String url,
                       @Value("${sptrans.gtfs.enabled:true}") boolean habilitado) {
        this.resourceLoader = resourceLoader;
        this.url = url;
        this.habilitado = habilitado;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void carregarAoIniciar() {
        if (habilitado) {
            CompletableFuture.runAsync(this::carregar);
        }
    }

    @Scheduled(cron = "${sptrans.gtfs.atualizacao-cron:0 30 4 * * *}", zone = "America/Sao_Paulo")
    public void atualizar() {
        if (habilitado) {
            carregar();
        }
    }

    void carregar() {
        long inicio = System.nanoTime();
        try (InputStream zip = abrir()) {
            GtfsDados novos = GtfsLeitor.ler(zip);
            dados.set(novos);
            log.info("GTFS carregado: {} paradas, {} itinerários em {} ms", novos.totalParadas(),
                    novos.totalItinerarios(), (System.nanoTime() - inicio) / 1_000_000);
        } catch (IOException | RuntimeException exception) {
            log.error("Falha ao carregar o GTFS de {}: {}", url, exception.toString());
        }
    }

    private InputStream abrir() throws IOException {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            URLConnection conexao = URI.create(url).toURL().openConnection();
            conexao.setConnectTimeout(TIMEOUT_CONEXAO_MS);
            conexao.setReadTimeout(TIMEOUT_LEITURA_MS);
            return conexao.getInputStream();
        }
        Resource recurso = resourceLoader.getResource(url);
        return recurso.getInputStream();
    }

    @Override
    public boolean disponivel() {
        return dados.get() != null;
    }

    @Override
    public Optional<Itinerario> buscarItinerario(String letreiro, int sentido) {
        GtfsDados atuais = dados.get();
        return atuais == null ? Optional.empty() : atuais.itinerario(letreiro, sentido);
    }

    @Override
    public List<ParadaProxima> buscarParadasProximas(double latitude, double longitude, int raioMetros, int limite) {
        GtfsDados atuais = dados.get();
        return atuais == null ? List.of() : atuais.paradasProximas(latitude, longitude, raioMetros, limite);
    }
}
