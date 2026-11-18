package frete;

public class PedidoReposicao {

    private final double valorInsumos;
    private CalculoFrete calculoFrete;

    public PedidoReposicao(double valorInsumos, CalculoFrete calculoFrete) {
        this.valorInsumos = valorInsumos;
        this.calculoFrete = calculoFrete;
    }

    public void setCalculoFrete(CalculoFrete calculoFrete){
        this.calculoFrete = calculoFrete;
    }

    public double calcularTotal(){
        return this.valorInsumos + calculoFrete.calcular();
    }
}
