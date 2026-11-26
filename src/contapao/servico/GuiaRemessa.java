package contapao.servico;

import java.util.Locale;

public record GuiaRemessa(String codigo, String codigoFilial, String resumoItens, double custoTotal) {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    public String formatar() {
        return "GUIA DE REMESSA " + codigo + "\n"
                + "Destino: Filial " + codigoFilial + "\n"
                + "Itens: " + resumoItens + "\n"
                + String.format(PT_BR, "Custo total: R$ %.2f", custoTotal);
    }
}
