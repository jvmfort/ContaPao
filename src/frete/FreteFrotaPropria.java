package frete;

public class FreteFrotaPropria implements CalculoFrete{

    private final double custo = 15.00;

    @Override
    public double calcular(){
        return custo;
    }
}
