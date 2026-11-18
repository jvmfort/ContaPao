package catalogo;

public class Insumo implements ItemReposicao{

    private String nome;
    private double custo;

    public Insumo(String nome, double custo) {
        this.nome = nome;
        this.custo = custo;
    }

    public double calcularCusto(){
        return custo;
    }
}
