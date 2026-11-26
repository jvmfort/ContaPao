package contapao.integracao;

public class MoinhoLegadoAdapter implements FornecedorInsumo {

    private final ApiMoinhoLegado apiMoinhoLegado;

    public MoinhoLegadoAdapter(ApiMoinhoLegado apiMoinhoLegado) {
        this.apiMoinhoLegado = apiMoinhoLegado;
    }

    @Override
    public boolean solicitarInsumo(String insumo, int quantidade) {
        if (quantidade <= 0) {
            return false;
        }
        String resposta = apiMoinhoLegado.enviarPedidoFarinha(insumo, (double) quantidade);
        return resposta != null && resposta.startsWith("CONFIRMADO");
    }
}
