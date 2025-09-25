package RadarSptrans.example.RadarSPT.resource;

import RadarSptrans.example.RadarSPT.client.SpTransAuthClient;
import RadarSptrans.example.RadarSPT.client.SpTransClient;
import RadarSptrans.example.RadarSPT.response.LinhaResponse;
import RadarSptrans.example.RadarSPT.response.PosicaoBus;
import RadarSptrans.example.RadarSPT.response.PosicaoBusResponse;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpTransControllerTest {

    @Mock
    private SpTransAuthClient authClient;

    @Mock
    private SpTransClient spTransClient;

    @InjectMocks
    private SpTransController controller;

    private Response authSuccessResponseWithCookie;
    private Response authSuccessResponseWithoutCookie;
    private Response authFailureResponse;

    @BeforeEach
    void setUp() {
        Request request = Request.create(
                Request.HttpMethod.POST,
                "/Autenticar",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                null
        );

        authSuccessResponseWithCookie = Response.builder()
                .status(200)
                .reason("OK")
                .request(request)
                .headers(Map.of("Set-Cookie", List.of("SESSION=123")))
                .build();

        authSuccessResponseWithoutCookie = Response.builder()
                .status(200)
                .reason("OK")
                .request(request)
                .headers(Collections.emptyMap())
                .build();

        authFailureResponse = Response.builder()
                .status(401)
                .reason("Unauthorized")
                .request(request)
                .headers(Collections.emptyMap())
                .build();
    }

    @Test
    void buscarLinhasReturnsBusPositionWhenIndexIsValid() {
        List<LinhaResponse> linhas = List.of(new LinhaResponse(123, false, "8000", 1, 1, "Terminal", "Bairro"));
        PosicaoBusResponse expectedResponse = new PosicaoBusResponse("10:00", List.of(new PosicaoBus("bus", true, null, -23.0, -46.0, null, null)));

        when(authClient.autenticar(org.mockito.ArgumentMatchers.anyString())).thenReturn(authSuccessResponseWithCookie);
        when(spTransClient.buscarLinha("term", "SESSION=123")).thenReturn(linhas);
        when(spTransClient.localBus("123", "SESSION=123")).thenReturn(expectedResponse);

        PosicaoBusResponse result = controller.buscarLinhas("term", 1);

        assertEquals(expectedResponse, result);
    }

    @Test
    void buscarLinhasThrowsExceptionWhenIndexIsInvalid() {
        List<LinhaResponse> linhas = List.of(new LinhaResponse(123, false, "8000", 1, 1, "Terminal", "Bairro"));

        when(authClient.autenticar(org.mockito.ArgumentMatchers.anyString())).thenReturn(authSuccessResponseWithCookie);
        when(spTransClient.buscarLinha("term", "SESSION=123")).thenReturn(linhas);

        assertThrows(IllegalArgumentException.class, () -> controller.buscarLinhas("term", 2));
    }

    @Test
    void buscarLinhasThrowsExceptionWhenCookieMissing() {
        when(authClient.autenticar(org.mockito.ArgumentMatchers.anyString())).thenReturn(authSuccessResponseWithoutCookie);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> controller.buscarLinhas("term", 1));
        assertTrue(exception.getMessage().contains("Falha ao capturar o cookie"));
    }

    @Test
    void buscarLinhasThrowsExceptionWhenAuthenticationFails() {
        when(authClient.autenticar(org.mockito.ArgumentMatchers.anyString())).thenReturn(authFailureResponse);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> controller.buscarLinhas("term", 1));
        assertTrue(exception.getMessage().contains("Autenticação falhou"));
    }

    @Test
    void localBusReturnsBusPositionWhenAuthenticationSucceeds() {
        PosicaoBusResponse expectedResponse = new PosicaoBusResponse("10:00", List.of(new PosicaoBus("bus", true, null, -23.0, -46.0, null, null)));

        when(authClient.autenticar(org.mockito.ArgumentMatchers.anyString())).thenReturn(authSuccessResponseWithCookie);
        when(spTransClient.localBus("123", "SESSION=123")).thenReturn(expectedResponse);

        PosicaoBusResponse result = controller.localBus("123");

        assertEquals(expectedResponse, result);
    }

    @Test
    void localBusThrowsExceptionWhenCookieMissing() {
        when(authClient.autenticar(org.mockito.ArgumentMatchers.anyString())).thenReturn(authSuccessResponseWithoutCookie);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> controller.localBus("123"));
        assertTrue(exception.getMessage().contains("Falha ao capturar o cookie"));
    }

    @Test
    void localBusThrowsExceptionWhenAuthenticationFails() {
        when(authClient.autenticar(org.mockito.ArgumentMatchers.anyString())).thenReturn(authFailureResponse);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> controller.localBus("123"));
        assertTrue(exception.getMessage().contains("Autenticação falhou"));
    }
}
