package RadarSptrans.example.RadarSPT.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LinhaResponse {
    private int cl;
    private boolean lc;
    private String lt;
    private int sl;
    private int tl;
    private String tp;
    private String ts;
}
