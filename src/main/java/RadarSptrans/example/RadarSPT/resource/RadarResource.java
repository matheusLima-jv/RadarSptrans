package RadarSptrans.example.RadarSPT.resource;

import RadarSptrans.example.RadarSPT.response.LinhasLocais;
import RadarSptrans.example.RadarSPT.usecase.GetRadarClientUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/localiza")
@RequiredArgsConstructor
public class RadarResource {

    private final GetRadarClientUseCase getRadarClientUseCase;

    @GetMapping("/{bus_id}/{sl}")
    public LinhasLocais linhasLocais(@PathVariable("bus_id") String bus_id, @PathVariable("sl") int sl) {
        return getRadarClientUseCase.execute(bus_id, sl);
    }

}
