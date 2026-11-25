package acesso;


/** Permissões por perfil, verificadas na camada de serviço (separadas do Observer). */
public interface PerfilAcesso {

    String getNome();

    boolean podeRegistrarSolicitacao(String codigoPadaria);

    boolean podeConsultarPadaria(String codigoPadaria);

    boolean podeGerenciarCiclo();

    boolean podeConfirmarDistribuicao();

    default void exigirRegistroSolicitacao(String codigoPadaria) {
        if (!podeRegistrarSolicitacao(codigoPadaria)) {
            throw new SecurityException(getNome() + " não pode registrar solicitação para a padaria " + codigoPadaria + ".");
        }
    }

    default void exigirGerenciarCiclo() {
        if (!podeGerenciarCiclo()) {
            throw new SecurityException(getNome() + " não pode gerenciar o ciclo.");
        }
    }

    default void exigirConfirmarDistribuicao() {
        if (!podeConfirmarDistribuicao()) {
            throw new SecurityException(getNome() + " não pode confirmar a distribuição.");
        }
    }
}
