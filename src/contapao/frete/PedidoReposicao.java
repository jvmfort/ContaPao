package contapao.frete;

import java.util.Objects;

public class PedidoReposicao {
    private final double valorInsumos;
    private CalculoFrete calculoFrete;

    public PedidoReposicao(double valorInsumos, CalculoFrete calculoFrete) {
        this.valorInsumos = valorInsumos;
        this.calculoFrete = Objects.requireNonNull(calculoFrete);
    }

    public void setCalculoFrete(CalculoFrete calculoFrete) {
        this.calculoFrete = Objects.requireNonNull(calculoFrete);
    }

    public double calcularTotal() {
        return valorInsumos + calculoFrete.calcular();
    }
}
