package contapao.servico;

import contapao.persistencia.RepositorioReposicao;

import java.util.Objects;

public class EstoqueMatrizService {

    private final RepositorioReposicao repositorio;

    public EstoqueMatrizService(RepositorioReposicao repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "Repositório não pode ser nulo.");
    }

    public void cadastrarEstoque(String insumo, int quantidade) {
        repositorio.cadastrarEstoque(insumo, quantidade);
    }

    public int consultarSaldo(String insumo) {
        return repositorio.consultarSaldoCentral(insumo);
    }

    public boolean validarDisponibilidade(String insumo, int solicitado, int atendido) {
        if (solicitado < 0 || atendido < 0 || atendido > solicitado) {
            return false;
        }
        return atendido <= consultarSaldo(insumo);
    }

    public boolean baixarEstoque(String insumo, int quantidade) {
        if (quantidade <= 0) {
            return false;
        }
        return repositorio.baixarSaldoCentral(insumo, quantidade);
    }
}