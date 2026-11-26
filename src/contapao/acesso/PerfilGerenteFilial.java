package contapao.acesso;

/** Gerente: registra e consulta apenas a própria padaria (RN02). */
public class PerfilGerenteFilial implements PerfilAcesso {

    private final String nome;
    private final String codigoPadaria;

    public PerfilGerenteFilial(String nome, String codigoPadaria) {
        this.nome = nome;
        this.codigoPadaria = codigoPadaria;
    }

    public String getCodigoPadaria() {
        return codigoPadaria;
    }

    @Override
    public String getNome() {
        return "Gerente " + nome;
    }

    @Override
    public boolean podeRegistrarSolicitacao(String codigo) {
        return codigoPadaria.equals(codigo);
    }

    @Override
    public boolean podeConsultarPadaria(String codigo) {
        return codigoPadaria.equals(codigo);
    }

    @Override
    public boolean podeGerenciarCiclo() {
        return false;
    }

    @Override
    public boolean podeConfirmarDistribuicao() {
        return false;
    }
}
