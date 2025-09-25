package RadarSptrans.example.RadarSPT.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PosicaoBusResponse {

    private String hr;
    private List<PosicaoBus> vs;
}
