package io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans;

import feign.Retryer;
import org.springframework.context.annotation.Bean;

/**
 * Configuração só do cliente de consultas (sem @Configuration de propósito: se fosse escaneada pelo
 * Spring, valeria também para o login, que é POST e não deve ser repetido).
 */
class SpTransClientConfig {

    // Uma nova tentativa após falha de rede/timeout (RetryableException). Erros HTTP não são repetidos.
    @Bean
    Retryer spTransRetryer() {
        return new Retryer.Default(200, 1000, 2);
    }
}
