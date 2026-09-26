package RadarSptrans.example.RadarSPT.application.service;

import RadarSptrans.example.RadarSPT.domain.exception.SessaoExpiradaException;
import RadarSptrans.example.RadarSPT.domain.port.out.AutenticacaoPort;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class SessaoSpTrans {

    private final AutenticacaoPort autenticacaoPort;

    public SessaoSpTrans(AutenticacaoPort autenticacaoPort) {
        this.autenticacaoPort = autenticacaoPort;
    }

    // A sessão fica em cache no adaptador de autenticação; se a SPTrans recusar o cookie,
    // descarta a sessão e tenta uma única vez com um cookie novo.
    public <T> T executar(Function<String, T> chamada) {
        try {
            return chamada.apply(autenticacaoPort.autenticar());
        } catch (SessaoExpiradaException exception) {
            autenticacaoPort.invalidarSessao();
            return chamada.apply(autenticacaoPort.autenticar());
        }
    }
}
