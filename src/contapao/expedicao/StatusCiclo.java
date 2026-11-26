package contapao.expedicao;

public enum StatusCiclo {
    ABERTO, ENCERRADO, DISTRIBUIDO;

    public boolean podeDistribuir(){
        return this == ENCERRADO;
    }
}
