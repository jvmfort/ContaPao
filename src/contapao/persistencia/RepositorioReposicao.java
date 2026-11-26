package contapao.persistencia;

import contapao.expedicao.StatusCiclo;

import java.util.Map;

public interface RepositorioReposicao {

    void inicializarBanco();

    void resetarDadosDemonstracao();

    int consultarSaldoCentral(String insumo);

    void cadastrarEstoque(String insumo, int quantidade);

    boolean baixarSaldoCentral(String insumo, int quantidade);

    void criarCiclo(int cicloId);

    void encerrarCiclo(int cicloId);

    StatusCiclo consultarEstadoCiclo(int cicloId);

    void registrarDistribuicaoAtomica(int cicloId, String insumo, int totalDistribuido,
                                      Map<String, Integer> atendimentosPorUnidade);

    Map<String, Integer> consultarAtendimentos(int cicloId, String insumo);
}