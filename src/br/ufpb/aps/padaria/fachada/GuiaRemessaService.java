package br.ufpb.aps.padaria.fachada;

public class GuiaRemessaService {

    public String gerarGuia(String codigoFilial, String resumoItens, double custoTotal) {
        String codigoGuia = "REM-" + System.currentTimeMillis() % 100000;
        System.out.println("[GuiaRemessaService] Guia de remessa emitida com sucesso!");
        System.out.println("  -> Codigo: " + codigoGuia);
        System.out.println("  -> Destino: Filial " + codigoFilial);
        System.out.println("  -> Itens: " + resumoItens);
        System.out.println("  -> Custo Total: R$ " + String.format("%.2f", custoTotal));
        return codigoGuia;
    }
}
