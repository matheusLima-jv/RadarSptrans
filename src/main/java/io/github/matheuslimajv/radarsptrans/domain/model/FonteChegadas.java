package io.github.matheuslimajv.radarsptrans.domain.model;

public enum FonteChegadas {
    /** Previsão oficial da SPTrans para a parada (considera o trânsito). */
    PREVISAO_SPTRANS,
    /**
     * A SPTrans não publica previsão para esta parada: estimativa pela posição atual de cada ônibus
     * mais o tempo de percurso programado no GTFS até a parada (não considera o trânsito).
     */
    POSICAO_VEICULOS,
    /** Nenhum ônibus da linha a caminho da parada. */
    SEM_DADOS
}
