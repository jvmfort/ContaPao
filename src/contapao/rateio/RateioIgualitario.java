package contapao.rateio;

import java.util.LinkedHashMap;
import java.util.Map;

public class RateioIgualitario implements PoliticaRateio {

    @Override
    public Map<String, Integer> calcularDistribuicao(int estoqueDisponivel, Map<String, Integer> demandasPorUnidade) {
        if (estoqueDisponivel < 0) {
            throw new IllegalArgumentException("Estoque disponível não pode ser negativo.");
        }

        Map<String, Integer> resultado = new LinkedHashMap<>();
        for (String unidade : demandasPorUnidade.keySet()) {
            resultado.put(unidade, 0);
        }

        int saldo = estoqueDisponivel;
        boolean houveAlocacao = true;

        // Distribui 1 unidade por rodada para cada filial que ainda não atingiu o seu pedido
        while (saldo > 0 && houveAlocacao) {
            houveAlocacao = false;
            for (Map.Entry<String, Integer> entry : demandasPorUnidade.entrySet()) {
                if (saldo == 0) break;
                String unidade = entry.getKey();
                int solicitado = entry.getValue();
                int atual = resultado.get(unidade);

                if (atual < solicitado) {
                    resultado.put(unidade, atual + 1);
                    saldo--;
                    houveAlocacao = true;
                }
            }
        }

        return resultado;
    }

    @Override
    public String getNomePolitica() {
        return "Cota Igualitária com Teto (Extensão)";
    }
}
