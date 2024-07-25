package RadarSptrans.example.RadarSPT.gateway;

import RadarSptrans.example.RadarSPT.response.BusInfo;
import RadarSptrans.example.RadarSPT.response.LinhasLocais;

public interface GetRadarClientGateway {

    LinhasLocais linhasLocais(int cl);

    BusInfo[] busInfo(String bus_id);
}
