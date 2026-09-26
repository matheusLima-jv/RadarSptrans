package io.github.matheuslimajv.radarsptrans.application.service;

import io.github.matheuslimajv.radarsptrans.domain.exception.SessaoExpiradaException;
import io.github.matheuslimajv.radarsptrans.domain.port.out.AutenticacaoPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessaoSpTransTest {

    private AutenticacaoPort autenticacaoPort;
    private SessaoSpTrans sessao;

    @BeforeEach
    void setUp() {
        autenticacaoPort = mock(AutenticacaoPort.class);
        sessao = new SessaoSpTrans(autenticacaoPort);
    }

    @Test
    void deveExecutarComCookieDaSessao() {
        when(autenticacaoPort.autenticar()).thenReturn("cookie");

        assertEquals("resultado cookie", sessao.executar(cookie -> "resultado " + cookie));
        verify(autenticacaoPort, never()).invalidarSessao();
    }

    @Test
    void deveRenovarSessaoETentarNovamenteQuandoSessaoExpira() {
        when(autenticacaoPort.autenticar()).thenReturn("antigo", "novo");

        String resultado = sessao.executar(cookie -> {
            if (cookie.equals("antigo")) {
                throw new SessaoExpiradaException();
            }
            return cookie;
        });

        assertEquals("novo", resultado);
        verify(autenticacaoPort).invalidarSessao();
    }

    @Test
    void deveDesistirQuandoSessaoExpiraNovamenteAposRenovar() {
        when(autenticacaoPort.autenticar()).thenReturn("antigo", "novo");

        assertThrows(SessaoExpiradaException.class, () -> sessao.executar(cookie -> {
            throw new SessaoExpiradaException();
        }));
        verify(autenticacaoPort, times(2)).autenticar();
    }
}
