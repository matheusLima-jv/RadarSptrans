package io.github.matheuslimajv.radarsptrans.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String LINHAS = "linhas";
    public static final String POSICAO_LINHA = "posicaoLinha";
    public static final String PARADAS_COM_PREVISAO = "paradasComPrevisao";
    public static final String PREVISAO = "previsao";

    // Posições da SPTrans têm idade mediana de ~20s (p90 ~40s): 10s de cache quase não piora o frescor.
    // Previsões são por minuto (HH:mm); linhas e paradas mudam raramente.
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache(LINHAS, cache(Duration.ofHours(1), 1_000));
        manager.registerCustomCache(POSICAO_LINHA, cache(Duration.ofSeconds(10), 2_000));
        manager.registerCustomCache(PARADAS_COM_PREVISAO, cache(Duration.ofHours(6), 2_000));
        manager.registerCustomCache(PREVISAO, cache(Duration.ofSeconds(15), 5_000));
        return manager;
    }

    private static com.github.benmanes.caffeine.cache.Cache<Object, Object> cache(Duration ttl, long maximo) {
        return Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(maximo).recordStats().build();
    }
}
