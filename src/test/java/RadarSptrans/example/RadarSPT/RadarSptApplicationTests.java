package RadarSptrans.example.RadarSPT;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Sem download do GTFS nos testes: o contexto deve subir sem rede.
@SpringBootTest(properties = "sptrans.gtfs.enabled=false")
class RadarSptApplicationTests {

	@Test
	void contextLoads() {
	}

}
