package rateio;

import java.util.LinkedHashMap;
import java.util.Map;

public class RateioProporcional implements PoliticaRateio {

    @Override
    public Map<String, Integer> calcularDistribuicao(int estoqueDisponivel, Map<String, Integer> demandasPorUnidade) {
        if (estoqueDisponivel < 0) {
            throw new IllegalArgumentException("Estoque disponível não pode ser negativo.");
        }

        Map<String, Integer> resultado = new LinkedHashMap<>();
        long demandaTotal = demandasPorUnidade.values().stream().mapToLong(Integer::intValue).sum();

        if (estoqueDisponivel == 0 || demandasPorUnidade.isEmpty()) {
            demandasPorUnidade.keySet().forEach(u -> resultado.put(u, 0));
            return resultado;
        }

        if (estoqueDisponivel >= demandaTotal) {
            resultado.putAll(demandasPorUnidade);
            return resultado;
        }

        long totalAlocado = 0;
        for (Map.Entry<String, Integer> entry : demandasPorUnidade.entrySet()) {
            int cota = (int) (((long) estoqueDisponivel * entry.getValue()) / demandaTotal);
            resultado.put(entry.getKey(), cota);
            totalAlocado += cota;
        }

        long residuo = estoqueDisponivel - totalAlocado;
        while (residuo > 0) {
            String escolhido = null;
            int maiorDemanda = -1;

            for (Map.Entry<String, Integer> entry : demandasPorUnidade.entrySet()) {
                String unidade = entry.getKey();
                int solicitado = entry.getValue();
                int atendido = resultado.get(unidade);

                if (atendido < solicitado && solicitado > maiorDemanda) {
                    maiorDemanda = solicitado;
                    escolhido = unidade;
                }
            }

            if (escolhido == null) {
                break;
            }
            resultado.put(escolhido, resultado.get(escolhido) + 1);
            residuo--;
        }

        return resultado;
    }

    @Override
    public String getNomePolitica() {
        return "Proporcional com Truncamento Inteiro";
    }
}