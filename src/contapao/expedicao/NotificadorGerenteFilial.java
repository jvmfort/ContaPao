package contapao.expedicao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NotificadorGerenteFilial implements ObservadorExpedicao {

    private final String nomeGerente;
    private final String codigoPadaria;
    private final List<String> mensagens = Collections.synchronizedList(new ArrayList<>());

    public NotificadorGerenteFilial(String nomeGerente, String codigoPadaria) {
        if (nomeGerente == null || nomeGerente.isBlank()) {
            throw new IllegalArgumentException("O nome do gerente é obrigatório.");
        }
        if (codigoPadaria == null || !codigoPadaria.matches("[ABC]")) {
            throw new IllegalArgumentException("A padaria deve ser A, B ou C.");
        }
        this.nomeGerente = nomeGerente;
        this.codigoPadaria = codigoPadaria;
    }

    public String getNomeGerente() {
        return nomeGerente;
    }

    public String getCodigoPadaria() {
        return codigoPadaria;
    }

    @Override
    public void aoEncerrarCiclo(int cicloId) {
        notificar("Ciclo " + cicloId + " encerrado.");
    }

    @Override
    public void aoDistribuirCiclo(int cicloId, String insumo, Map<String, Integer> atendimentos) {
        String chavePadaria = "Padaria " + codigoPadaria;
        Integer recebido = atendimentos.get(chavePadaria);
        if (recebido != null) {
            notificar("Ciclo " + cicloId + ": " + chavePadaria + " recebeu " + recebido + " de " + insumo + ".");
        }
    }

    private void notificar(String mensagem) {
        mensagens.add(mensagem);
        System.out.println("[Gerente " + nomeGerente + " - Padaria " + codigoPadaria + "] " + mensagem);
    }

    public List<String> getMensagens() {
        synchronized (mensagens) {
            return List.copyOf(mensagens);
        }
    }
}