package RadarSptrans.example.RadarSPT.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class BusInfo {

    private int cl;
    private boolean lc;
    private String lt;
    private int sl;
    private int tl;
    private String tp;
    private String ts;

}
