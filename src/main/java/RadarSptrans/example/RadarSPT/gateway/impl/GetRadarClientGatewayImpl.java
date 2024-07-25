package RadarSptrans.example.RadarSPT.gateway.impl;

import RadarSptrans.example.RadarSPT.client.RadarClient;
import RadarSptrans.example.RadarSPT.gateway.GetRadarClientGateway;
import RadarSptrans.example.RadarSPT.response.BusInfo;
import RadarSptrans.example.RadarSPT.response.LinhasLocais;
import RadarSptrans.example.RadarSPT.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class GetRadarClientGatewayImpl implements GetRadarClientGateway {

    private final RadarClient radarClient;
    private final AuthService authService;

    @Override
    public LinhasLocais linhasLocais(int cl) {
        return radarClient.linhasLocais(cl);
    }

    @Override
    public BusInfo[] busInfo(String bus_id) {
        return radarClient.busInfo(bus_id);
    }


}
