package io.github.matheuslimajv.radarsptrans.domain.model;

/**
 * @param segundosDesdeInicio tempo de percurso programado desde a primeira parada do itinerário
 */
public record ParadaItinerario(Parada parada, int segundosDesdeInicio) {
}
