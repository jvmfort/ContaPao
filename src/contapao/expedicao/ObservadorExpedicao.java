package contapao.expedicao;

import java.util.Map;

public interface ObservadorExpedicao {

    void aoEncerrarCiclo(int cicloId);

    void aoDistribuirCiclo(int cicloId, String insumo, Map<String, Integer> atendimentos);
}
