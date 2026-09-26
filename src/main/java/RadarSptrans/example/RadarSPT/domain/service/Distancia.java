package RadarSptrans.example.RadarSPT.domain.service;

public final class Distancia {

    private static final double RAIO_TERRA_METROS = 6_371_000;

    private Distancia() {
    }

    // Haversine: distância em linha reta sobre a superfície da Terra, não o caminho a pé.
    public static double metros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RAIO_TERRA_METROS * Math.asin(Math.sqrt(a));
    }
}
