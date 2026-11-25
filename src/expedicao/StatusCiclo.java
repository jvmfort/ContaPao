package expedicao;

/** Estados do ciclo: ABERTO -> ENCERRADO -> DISTRIBUIDO, sem voltar atrás. */
public enum StatusCiclo {
    ABERTO, ENCERRADO, DISTRIBUIDO;

    /** Próximo estado válido, ou null se o ciclo já está no estado final. */
    public StatusCiclo proximo() {
        switch (this) {
            case ABERTO: return ENCERRADO;
            case ENCERRADO: return DISTRIBUIDO;
            default: return null;
        }
    }

    public boolean podeTransitarPara(StatusCiclo destino) {
        return destino != null && destino == proximo();
    }

    public boolean aceitaSolicitacoes() {
        return this == ABERTO;
    }
}
