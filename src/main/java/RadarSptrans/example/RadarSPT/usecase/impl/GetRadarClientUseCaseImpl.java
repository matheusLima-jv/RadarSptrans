package RadarSptrans.example.RadarSPT.usecase.impl;

import RadarSptrans.example.RadarSPT.gateway.GetRadarClientGateway;
import RadarSptrans.example.RadarSPT.response.BusInfo;
import RadarSptrans.example.RadarSPT.response.LinhasLocais;
import RadarSptrans.example.RadarSPT.service.AuthService;
import RadarSptrans.example.RadarSPT.usecase.GetRadarClientUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class GetRadarClientUseCaseImpl implements GetRadarClientUseCase {

    private final GetRadarClientGateway getRadarClientGateway;
    private final AuthService authService;

    @Override
    public LinhasLocais execute(String bus_id, int sl) {
        BusInfo[] busInfos = getRadarClientGateway.busInfo(bus_id);
        for (BusInfo busInfo : busInfos) {
            if (busInfo.getSl() == sl) {
                return getRadarClientGateway.linhasLocais(busInfo.getCl());
            }
        }

        throw new IllegalArgumentException("No bus info found for the given sl value");
    }
}
