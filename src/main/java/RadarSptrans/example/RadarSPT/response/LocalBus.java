package RadarSptrans.example.RadarSPT.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class LocalBus {

    private String p;
    private boolean a;
    private Date ta;
    private double py;
    private double px;
    private Object sv;
    private Object is;

}
