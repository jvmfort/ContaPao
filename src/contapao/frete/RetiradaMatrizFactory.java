package contapao.frete;

public class RetiradaMatrizFactory extends FreteFactory {

    @Override
    public CalculoFrete criarFrete(){
        return new RetiradaMatriz();
    }
}
