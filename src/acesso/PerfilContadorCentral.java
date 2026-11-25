package acesso;

/** Contador: gerencia o ciclo, consolida e confirma; não registra solicitação. */
public class PerfilContadorCentral implements PerfilAcesso {

    private final String nome;

    public PerfilContadorCentral(String nome) {
        this.nome = nome;
    }

    @Override
    public String getNome() {
        return "Contador " + nome;
    }

    @Override
    public boolean podeRegistrarSolicitacao(String codigo) {
        return false;
    }

    @Override
    public boolean podeConsultarPadaria(String codigo) {
        return true;
    }

    @Override
    public boolean podeGerenciarCiclo() {
        return true;
    }

    @Override
    public boolean podeConfirmarDistribuicao() {
        return true;
    }
}
