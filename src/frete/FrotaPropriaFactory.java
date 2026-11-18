package frete;

public class FrotaPropriaFactory extends FreteFactory {

    @Override
    public CalculoFrete criarFrete(){
        return new FreteFrotaPropria();
    }
}
