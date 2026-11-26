package contapao.expedicao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NotificadorPainelCentral implements ObservadorExpedicao {

    private final List<String> historico = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void aoEncerrarCiclo(int cicloId) {
        registrar("Ciclo " + cicloId + " encerrado.");
    }

    @Override
    public void aoDistribuirCiclo(int cicloId, String insumo, Map<String, Integer> atendimentos) {
        registrar("Ciclo " + cicloId + " distribuído (" + insumo + "): " + atendimentos);
    }

    private void registrar(String mensagem) {
        historico.add(mensagem);
        System.out.println("[Painel Central] " + mensagem);
    }

    public List<String> getHistorico() {
        synchronized (historico) {
            return List.copyOf(historico);
        }
    }
}