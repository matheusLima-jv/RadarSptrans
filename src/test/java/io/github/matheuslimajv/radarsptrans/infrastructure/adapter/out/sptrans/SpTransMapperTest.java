package io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans;

import io.github.matheuslimajv.radarsptrans.domain.model.ChegadaPrevista;
import io.github.matheuslimajv.radarsptrans.domain.model.Linha;
import io.github.matheuslimajv.radarsptrans.domain.model.PosicaoLinha;
import io.github.matheuslimajv.radarsptrans.domain.model.PrevisaoParada;
import io.github.matheuslimajv.radarsptrans.domain.model.Veiculo;
import io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans.dto.SpTransLinha;
import io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans.dto.SpTransPosicaoLinha;
import io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans.dto.SpTransPrevisao;
import io.github.matheuslimajv.radarsptrans.infrastructure.adapter.out.sptrans.dto.SpTransVeiculo;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpTransMapperTest {

    // Dados reais da 7545-10: sl 1 tem paradas "B/C" (bairro → centro) e vai para o terminal principal.
    @Test
    void sentidoUmVaiDoTerminalSecundarioParaOPrincipal() {
        Linha linha = SpTransMapper.linha(
                new SpTransLinha(504, false, "7545", 1, 10, "PÇA. RAMOS DE AZEVEDO", "JD. JOÃO XXIII"));

        assertEquals(new Linha(504, "7545-10", 1, "JD. JOÃO XXIII", "PÇA. RAMOS DE AZEVEDO", false), linha);
    }

    @Test
    void sentidoDoisVaiDoTerminalPrincipalParaOSecundario() {
        Linha linha = SpTransMapper.linha(
                new SpTransLinha(33272, false, "7545", 2, 10, "PÇA. RAMOS DE AZEVEDO", "JD. JOÃO XXIII"));

        assertEquals("PÇA. RAMOS DE AZEVEDO", linha.origem());
        assertEquals("JD. JOÃO XXIII", linha.destino());
    }

    @Test
    void deveConverterPosicoesTratandoListaNula() {
        Instant atualizadoEm = Instant.parse("2024-01-01T10:00:00Z");
        SpTransPosicaoLinha posicao = new SpTransPosicaoLinha("07:00",
                List.of(new SpTransVeiculo("12644", true, atualizadoEm, -23.5, -46.6)));

        assertEquals(new PosicaoLinha("07:00", List.of(new Veiculo("12644", true, atualizadoEm, -23.5, -46.6))),
                SpTransMapper.posicao(posicao));
        assertEquals(new PosicaoLinha("07:00", List.of()), SpTransMapper.posicao(new SpTransPosicaoLinha("07:00", null)));
    }

    @Test
    void previsaoConsideraSomenteALinhaPedida() {
        SpTransPrevisao previsao = new SpTransPrevisao("19:55", new SpTransPrevisao.Parada(260015039L, "PAULISTA B/C",
                -23.5, -46.6, List.of(
                new SpTransPrevisao.Linha("7545-10", 504, 1, List.of(new SpTransPrevisao.Veiculo("81443", "20:12", true))),
                new SpTransPrevisao.Linha("178L-10", 33328, 2, List.of(new SpTransPrevisao.Veiculo("22905", "19:58", false))))));

        assertEquals(new PrevisaoParada("19:55", List.of(new ChegadaPrevista("81443", "20:12", true))),
                SpTransMapper.previsao(previsao, 504));
    }
}
