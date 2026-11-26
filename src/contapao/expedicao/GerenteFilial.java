package contapao.expedicao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GerenteFilial implements ObservadorExpedicao {

    private final String padaria;
    private final List<String> mensagens = Collections.synchronizedList(new ArrayList<>());

    public GerenteFilial(String padaria) {
        if (padaria == null || padaria.isBlank()) {
            throw new IllegalArgumentException("Padaria não informada.");
        }
        this.padaria = padaria;
    }

    public String getPadaria() {
        return padaria;
    }

    @Override
    public void aoEncerrarCiclo(int cicloId) {
        mensagens.add("Ciclo " + cicloId + " encerrado.");
    }

    @Override
    public void aoDistribuirCiclo(int cicloId, String insumo, Map<String, Integer> atendimentos) {
        Integer recebido = atendimentos.get(padaria);
        if (recebido != null) {
            mensagens.add("Ciclo " + cicloId + ": " + padaria + " recebeu " + recebido + " de " + insumo + ".");
        }
    }

    public List<String> getMensagens() {
        synchronized (mensagens) {
            return List.copyOf(mensagens);
        }
    }
}