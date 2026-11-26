package contapao.frete;

public class FreteRefrigerado implements CalculoFrete{

    private final double custo = 45.00;

    @Override
    public double calcular(){
        return custo;
    }
}
