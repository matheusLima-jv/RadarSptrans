package io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans;

import feign.Retryer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.FeignClientFactory;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

@SpringBootTest(properties = "sptrans.gtfs.enabled=false")
class SpTransClientConfigTest {

    @Autowired
    private FeignClientFactory feignClientFactory;

    @Test
    void consultasTentamNovamenteMasLoginNao() {
        assertInstanceOf(Retryer.Default.class, feignClientFactory.getInstance("spTransClient", Retryer.class));
        assertSame(Retryer.NEVER_RETRY, feignClientFactory.getInstance("authClient", Retryer.class));
    }
}
