package RadarSptrans.example.RadarSPT.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class RelogioConfig {

    // Horários da SPTrans (previsões, GTFS) são sempre no fuso de São Paulo, independente do servidor.
    @Bean
    public Clock relogioSaoPaulo() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}
