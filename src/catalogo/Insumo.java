package catalogo;

import java.util.Objects;

public class Insumo implements ItemReposicao {
    private final String nome;
    private final double custo;

    public Insumo(String nome, double custo) {
        if (custo < 0) throw new IllegalArgumentException("Custo não pode ser negativo.");
        this.nome = Objects.requireNonNull(nome);
        this.custo = custo;
    }

    public String getNome() { return nome; }

    @Override
    public double calcularCusto() { return custo; }
}