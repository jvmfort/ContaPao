package rateio;

import java.util.Map;

public interface PoliticaRateio {

    Map<String, Integer> calcularDistribuicao(int estoqueDisponivel, Map<String, Integer> demandasPorUnidade);
    String getNomePolitica();

}
