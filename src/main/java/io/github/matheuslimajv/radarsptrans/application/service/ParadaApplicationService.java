package io.github.matheuslimajv.radarsptrans.application.service;

import io.github.matheuslimajv.radarsptrans.domain.exception.DadosProgramadosIndisponiveisException;
import io.github.matheuslimajv.radarsptrans.domain.model.ChegadaEstimada;
import io.github.matheuslimajv.radarsptrans.domain.model.FonteChegadas;
import io.github.matheuslimajv.radarsptrans.domain.model.Itinerario;
import io.github.matheuslimajv.radarsptrans.domain.model.Linha;
import io.github.matheuslimajv.radarsptrans.domain.model.Parada;
import io.github.matheuslimajv.radarsptrans.domain.model.ParadaItinerario;
import io.github.matheuslimajv.radarsptrans.domain.model.ParadaProxima;
import io.github.matheuslimajv.radarsptrans.domain.model.PosicaoLinha;
import io.github.matheuslimajv.radarsptrans.domain.model.PrevisaoParada;
import io.github.matheuslimajv.radarsptrans.domain.model.TempoEsperaParada;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarLinhasUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.BuscarParadasProximasUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.in.CalcularTempoEsperaUseCase;
import io.github.matheuslimajv.radarsptrans.domain.port.out.DadosProgramadosPort;
import io.github.matheuslimajv.radarsptrans.domain.port.out.SpTransDadosPort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static io.github.matheuslimajv.radarsptrans.domain.service.CalculadoraTempoEspera.arredondar;
import static io.github.matheuslimajv.radarsptrans.domain.service.CalculadoraTempoEspera.chegadasPorPosicao;
import static io.github.matheuslimajv.radarsptrans.domain.service.CalculadoraTempoEspera.chegadasPrevistas;
import static io.github.matheuslimajv.radarsptrans.domain.service.CalculadoraTempoEspera.distanciaMetros;
import static io.github.matheuslimajv.radarsptrans.domain.service.CalculadoraTempoEspera.indiceParadaMaisProxima;
import static io.github.matheuslimajv.radarsptrans.domain.service.CalculadoraTempoEspera.intervaloMedioMinutos;

@Service
public class ParadaApplicationService implements BuscarParadasProximasUseCase, CalcularTempoEsperaUseCase {

    // Um termo amplo ("Lapa") casa com 150+ linhas; os dados em tempo real (até 3 chamadas à SPTrans
    // por linha) só são consultados para as linhas com parada mais perto do usuário.
    static final int MAXIMO_LINHAS_TEMPO_ESPERA = 4;

    private final BuscarLinhasUseCase buscarLinhasUseCase;
    private final DadosProgramadosPort dadosProgramados;
    private final SpTransDadosPort spTransDadosPort;
    private final SessaoSpTrans sessao;
    private final Clock relogio;

    public ParadaApplicationService(BuscarLinhasUseCase buscarLinhasUseCase, DadosProgramadosPort dadosProgramados,
                                    SpTransDadosPort spTransDadosPort, SessaoSpTrans sessao, Clock relogio) {
        this.buscarLinhasUseCase = buscarLinhasUseCase;
        this.dadosProgramados = dadosProgramados;
        this.spTransDadosPort = spTransDadosPort;
        this.sessao = sessao;
        this.relogio = relogio;
    }

    @Override
    public List<ParadaProxima> buscarParadasProximas(double latitude, double longitude, int raioMetros, int limite) {
        exigirDadosProgramados();
        return dadosProgramados.buscarParadasProximas(latitude, longitude, raioMetros, limite);
    }

    @Override
    public List<TempoEsperaParada> calcularTempoEspera(String termosBusca, Integer codigoLinha,
                                                       double latitude, double longitude) {
        exigirDadosProgramados();
        return buscarLinhasUseCase.buscarLinhas(termosBusca).stream()
                .filter(linha -> codigoLinha == null || linha.codigo() == codigoLinha)
                .map(linha -> localizarParada(linha, latitude, longitude))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingDouble(Candidata::distanciaMetros))
                .limit(MAXIMO_LINHAS_TEMPO_ESPERA)
                .map(this::calcular)
                .toList();
    }

    private Optional<Candidata> localizarParada(Linha linha, double latitude, double longitude) {
        return dadosProgramados.buscarItinerario(linha.letreiro(), linha.sentido())
                .filter(itinerario -> !itinerario.paradas().isEmpty())
                .map(itinerario -> {
                    int indice = indiceParadaMaisProxima(itinerario, latitude, longitude);
                    double distancia = distanciaMetros(itinerario.paradas().get(indice), latitude, longitude);
                    return new Candidata(linha, itinerario, indice, distancia);
                });
    }

    private TempoEsperaParada calcular(Candidata candidata) {
        Linha linha = candidata.linha();
        Itinerario itinerario = candidata.itinerario();
        ParadaItinerario alvo = itinerario.paradas().get(candidata.indice());

        Double intervaloProgramado = itinerario.intervaloProgramadoSegundos(LocalDateTime.now(relogio))
                .map(segundos -> arredondar(segundos / 60.0))
                .orElse(null);
        Double esperaMedia = intervaloProgramado != null ? arredondar(intervaloProgramado / 2) : null;

        List<ChegadaEstimada> chegadas = List.of();
        FonteChegadas fonte = FonteChegadas.SEM_DADOS;
        String horaReferencia = null;

        if (temPrevisaoOficial(linha, alvo.parada())) {
            PrevisaoParada previsao = sessao.executar(cookie ->
                    spTransDadosPort.buscarPrevisao(alvo.parada().codigo(), linha.codigo(), cookie));
            if (previsao.horaReferencia() != null && !previsao.chegadas().isEmpty()) {
                chegadas = chegadasPrevistas(previsao);
                fonte = FonteChegadas.PREVISAO_SPTRANS;
                horaReferencia = previsao.horaReferencia();
            }
        }
        if (chegadas.isEmpty()) {
            PosicaoLinha posicao = sessao.executar(cookie ->
                    spTransDadosPort.buscarPosicaoLinha(linha.codigo(), cookie));
            if (posicao.horaReferencia() != null) {
                chegadas = chegadasPorPosicao(itinerario, candidata.indice(), posicao, relogio.instant());
                horaReferencia = posicao.horaReferencia();
                fonte = chegadas.isEmpty() ? FonteChegadas.SEM_DADOS : FonteChegadas.POSICAO_VEICULOS;
            }
        }

        return new TempoEsperaParada(
                linha,
                alvo.parada(),
                (int) Math.round(candidata.distanciaMetros()),
                intervaloProgramado,
                esperaMedia,
                chegadas,
                chegadas.isEmpty() ? null : chegadas.get(0).minutos(),
                intervaloMedioMinutos(chegadas),
                fonte,
                horaReferencia);
    }

    // A SPTrans só publica previsão para parte das paradas de cada linha (na 7545-10, 11 de 39).
    private boolean temPrevisaoOficial(Linha linha, Parada parada) {
        return sessao.executar(cookie -> spTransDadosPort.buscarParadasComPrevisao(linha.codigo(), cookie))
                .stream()
                .anyMatch(comPrevisao -> comPrevisao.codigo() == parada.codigo());
    }

    private void exigirDadosProgramados() {
        if (!dadosProgramados.disponivel()) {
            throw new DadosProgramadosIndisponiveisException();
        }
    }

    private record Candidata(Linha linha, Itinerario itinerario, int indice, double distanciaMetros) {
    }
}
