package catalogo;

import java.util.ArrayList;
import java.util.List;

public class KitReposicao implements ItemReposicao {

    private String nome;
    private List<ItemReposicao> itens = new ArrayList<>();

    public KitReposicao(String nome) {
        this.nome = nome;
    }

    public void adicionar(ItemReposicao item){
        itens.add(item);
    }

    public void remover(ItemReposicao item){
        itens.remove(item);
    }

    @Override
    public double calcularCusto(){
        double custoTotal = 0.0;
        for (ItemReposicao item : itens){
            custoTotal += item.calcularCusto();
        }
        return custoTotal;
    }
}
