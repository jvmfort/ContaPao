package br.ufpb.aps.padaria.fachada;

public class FinanceiroInternoService {

    public boolean debitarContaFilial(String codigoFilial, double valorTotal) {
        if (valorTotal < 0) {
            System.out.println("[FinanceiroInternoService] Valor invalido para debito: " + valorTotal);
            return false;
        }
        System.out.println("[FinanceiroInternoService] Debitado R$ " + String.format("%.2f", valorTotal) +
                " da conta interna da filial: " + codigoFilial);
        return true;
    }
}
