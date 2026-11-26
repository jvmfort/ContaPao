package contapao.servico;

import java.util.HashMap;
import java.util.Map;

public class FinanceiroInternoService {

    private final Map<String, Double> debitosPorFilial = new HashMap<>();

    public synchronized boolean debitarContaFilial(String codigoFilial, double valorTotal) {
        if (codigoFilial == null || codigoFilial.isBlank() || valorTotal < 0 || !Double.isFinite(valorTotal)) {
            return false;
        }
        debitosPorFilial.merge(codigoFilial, valorTotal, Double::sum);
        return true;
    }

    public synchronized double totalDebitado(String codigoFilial) {
        return debitosPorFilial.getOrDefault(codigoFilial, 0.0);
    }
}
