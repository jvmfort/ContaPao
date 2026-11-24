package br.ufpb.aps.padaria.fachada;

import java.util.HashMap;
import java.util.Map;

public class EstoqueMatrizService {

    private final Map<String, Integer> saldoEstoque = new HashMap<>();

    public EstoqueMatrizService() {
        // Inicializa com insumos padrão do catálogo
        saldoEstoque.put("Farinha de Trigo Especial", 500);
        saldoEstoque.put("Fermento Biologico Seco", 200);
        saldoEstoque.put("Manteiga sem Sal", 150);
        saldoEstoque.put("Acucar Cristal", 300);
    }

    public void cadastrarEstoque(String insumo, int quantidade) {
        saldoEstoque.put(insumo, saldoEstoque.getOrDefault(insumo, 0) + quantidade);
    }

    public int consultarSaldo(String insumo) {
        return saldoEstoque.getOrDefault(insumo, 0);
    }

    /**
     * RN03: Valida se a quantidade atendida não ultrapassa a solicitada
     * e se a quantidade total não ultrapassa o saldo disponível na matriz.
     */
    public boolean validarDisponibilidade(String insumo, int solicitado, int atendido) {
        if (atendido <= 0 || solicitado <= 0) {
            return false;
        }

        // Validação RN03: atendido <= solicitado
        if (atendido > solicitado) {
            System.out.println("[EstoqueMatrizService] Erro RN03: Quantidade atendida (" + atendido +
                    ") ultrapassa a solicitada (" + solicitado + ").");
            return false;
        }

        // Validação RN03: total <= disponivel
        int disponivel = consultarSaldo(insumo);
        if (atendido > disponivel) {
            System.out.println("[EstoqueMatrizService] Erro RN03: Saldo insuficiente para '" + insumo +
                    "'. Solicitado/Atendido: " + atendido + " | Disponivel: " + disponivel);
            return false;
        }

        return true;
    }

    public boolean baixarEstoque(String insumo, int quantidade) {
        int saldoAtual = consultarSaldo(insumo);
        if (saldoAtual >= quantidade) {
            saldoEstoque.put(insumo, saldoAtual - quantidade);
            return true;
        }
        return false;
    }
}
