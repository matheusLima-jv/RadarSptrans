package RadarSptrans.example.RadarSPT.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class LinhasLocais {

    public String hr;
    public ArrayList<LocalBus> vs;
}
