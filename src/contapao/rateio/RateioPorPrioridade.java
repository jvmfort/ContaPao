package contapao.rateio;

import java.util.LinkedHashMap;
import java.util.Map;

public class RateioPorPrioridade implements PoliticaRateio {

    @Override
    public Map<String, Integer> calcularDistribuicao(int estoqueDisponivel, Map<String, Integer> demandasPorUnidade) {
        if (estoqueDisponivel < 0) {
            throw new IllegalArgumentException("Estoque disponível não pode ser negativo.");
        }

        Map<String, Integer> resultado = new LinkedHashMap<>();
        int saldoRestante = estoqueDisponivel;

        // A ordem de inserção no LinkedHashMap define a prioridade (ex: Padaria A -> B -> C)
        for (Map.Entry<String, Integer> entry : demandasPorUnidade.entrySet()) {
            int solicitado = entry.getValue();
            int atendido = Math.min(solicitado, saldoRestante);
            resultado.put(entry.getKey(), atendido);
            saldoRestante -= atendido;
        }

        return resultado;
    }

    @Override
    public String getNomePolitica() {
        return "Prioridade Sequencial de Unidades";
    }
}
