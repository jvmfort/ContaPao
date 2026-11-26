package contapao.integracao;

public class ApiMoinhoLegado {

    public String enviarPedidoFarinha(String tipoFarinha, double quantidadeSacos) {
        System.out.println("[ApiMoinhoLegado] Processando pedido legado: " + tipoFarinha + " (" + quantidadeSacos + " sacos)");
        return "CONFIRMADO_PEDIDO_" + (int) quantidadeSacos;
    }
}
