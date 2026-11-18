package frete;

public class FreteRefrigeradoFactory extends FreteFactory {

    @Override
    public CalculoFrete criarFrete(){
        return new FreteRefrigerado();
    }
}
