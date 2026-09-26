package RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans;

import RadarSptrans.example.RadarSPT.domain.model.ChegadaPrevista;
import RadarSptrans.example.RadarSPT.domain.model.Linha;
import RadarSptrans.example.RadarSPT.domain.model.Parada;
import RadarSptrans.example.RadarSPT.domain.model.PosicaoLinha;
import RadarSptrans.example.RadarSPT.domain.model.PrevisaoParada;
import RadarSptrans.example.RadarSPT.domain.model.Veiculo;
import RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans.dto.SpTransLinha;
import RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans.dto.SpTransParada;
import RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans.dto.SpTransPosicaoLinha;
import RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans.dto.SpTransPrevisao;
import RadarSptrans.example.RadarSPT.infrastructure.adapter.out.sptrans.dto.SpTransVeiculo;

import java.util.List;
import java.util.Objects;

/** Traduz o vocabulário da API Olho Vivo (cl, lt, tl, py, px...) para o modelo de domínio. */
final class SpTransMapper {

    private SpTransMapper() {
    }

    static List<Linha> linhas(List<SpTransLinha> linhas) {
        return linhas == null ? List.of() : linhas.stream().map(SpTransMapper::linha).toList();
    }

    // Apesar da documentação da SPTrans dizer que sl = 1 é "terminal principal → secundário", os dados mostram
    // o contrário: as paradas de sl = 1 coincidem com a viagem GTFS de direction_id 0, cujo trip_headsign
    // (destino) é o terminal principal, e são paradas "B/C" (bairro → centro) quando o principal fica no centro.
    static Linha linha(SpTransLinha linha) {
        boolean vaiParaPrincipal = linha.sl() == 1;
        return new Linha(
                linha.cl(),
                linha.lt() + "-" + linha.tl(),
                linha.sl(),
                vaiParaPrincipal ? linha.ts() : linha.tp(),
                vaiParaPrincipal ? linha.tp() : linha.ts(),
                linha.lc());
    }

    static PosicaoLinha posicao(SpTransPosicaoLinha posicao) {
        if (posicao == null) {
            return new PosicaoLinha(null, List.of());
        }
        List<SpTransVeiculo> veiculos = posicao.vs() == null ? List.of() : posicao.vs();
        return new PosicaoLinha(posicao.hr(), veiculos.stream()
                .map(v -> new Veiculo(v.p(), v.a(), v.ta(), v.py(), v.px()))
                .toList());
    }

    static List<Parada> paradas(List<SpTransParada> paradas) {
        return paradas == null ? List.of() : paradas.stream()
                .map(p -> new Parada(p.cp(), p.np(), p.ed(), p.py(), p.px()))
                .toList();
    }

    static PrevisaoParada previsao(SpTransPrevisao previsao, int codigoLinha) {
        if (previsao == null) {
            return PrevisaoParada.vazia(null);
        }
        if (previsao.p() == null || previsao.p().l() == null) {
            return PrevisaoParada.vazia(previsao.hr());
        }
        List<ChegadaPrevista> chegadas = previsao.p().l().stream()
                .filter(linha -> linha.cl() == codigoLinha)
                .map(SpTransPrevisao.Linha::vs)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(v -> new ChegadaPrevista(v.p(), v.t(), v.a()))
                .toList();
        return new PrevisaoParada(previsao.hr(), chegadas);
    }
}
