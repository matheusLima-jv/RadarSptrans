package RadarSptrans.example.RadarSPT.usecase;

import RadarSptrans.example.RadarSPT.response.LinhasLocais;

public interface GetRadarClientUseCase {

    LinhasLocais execute(String bus_id, int sl);

}
