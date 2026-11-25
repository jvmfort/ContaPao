package catalogo;

import java.util.Objects;

public class SolicitacaoReposicao {

    private final Padaria padaria;
    private final ItemReposicao item;
    private final int qtdSolicitada;
    private int qtdAtendida;

    public SolicitacaoReposicao(Padaria padaria, ItemReposicao item, int qtdSolicitada) {
        this.padaria = Objects.requireNonNull(padaria, "Padaria não pode ser nula.");
        this.item = Objects.requireNonNull(item, "Item de reposição não pode ser nulo.");
        if (qtdSolicitada < 0) {
            throw new IllegalArgumentException("Quantidade solicitada não pode ser negativa.");
        }
        this.qtdSolicitada = qtdSolicitada;
        this.qtdAtendida = 0;
    }

    public Padaria getPadaria() {
        return padaria;
    }

    public ItemReposicao getItem() {
        return item;
    }

    public int getQtdSolicitada() {
        return qtdSolicitada;
    }

    public int getQtdAtendida() {
        return qtdAtendida;
    }

    public void setQtdAtendida(int qtdAtendida) {
        if (qtdAtendida < 0) {
            throw new IllegalArgumentException("Quantidade atendida não pode ser negativa.");
        }
        if (qtdAtendida > this.qtdSolicitada) {
            throw new IllegalArgumentException(
                    "Quantidade atendida (" + qtdAtendida + ") não pode exceder a solicitada (" + this.qtdSolicitada + ")."
            );
        }
        this.qtdAtendida = qtdAtendida;
    }

    public int getSaldoPendente() {
        return qtdSolicitada - qtdAtendida;
    }
}