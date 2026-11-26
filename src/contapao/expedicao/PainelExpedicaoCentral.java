package contapao.expedicao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PainelExpedicaoCentral implements ObservadorExpedicao {

    private final List<String> historico = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void aoEncerrarCiclo(int cicloId) {
        historico.add("Ciclo " + cicloId + " encerrado.");
    }

    @Override
    public void aoDistribuirCiclo(int cicloId, String insumo, Map<String, Integer> atendimentos) {
        historico.add("Ciclo " + cicloId + " distribuído (" + insumo + "): " + atendimentos);
    }

    public List<String> getHistorico() {
        synchronized (historico) {
            return List.copyOf(historico);
        }
    }
}