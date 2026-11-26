package contapao.catalogo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class KitReposicao implements ItemReposicao {
    private final String nome;
    private final List<ItemReposicao> itens = new ArrayList<>();

    public KitReposicao(String nome) {
        this.nome = Objects.requireNonNull(nome);
    }

    public String getNome() { return nome; }

    public void adicionar(ItemReposicao item) {
        if (item == this) throw new IllegalArgumentException("Um kit não pode conter a si mesmo.");
        itens.add(Objects.requireNonNull(item));
    }

    public void remover(ItemReposicao item) { itens.remove(item); }

    public List<ItemReposicao> getItens() { return Collections.unmodifiableList(itens); }

    @Override
    public double calcularCusto() {
        return itens.stream().mapToDouble(ItemReposicao::calcularCusto).sum();
    }
}
