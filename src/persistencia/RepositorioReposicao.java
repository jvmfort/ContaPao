package persistencia;

import expedicao.StatusCiclo;
import java.util.Map;

public interface RepositorioReposicao {
    void inicializarBanco();
    void resetarDadosDemonstracao();
    int consultarSaldoCentral(String insumo);
    StatusCiclo consultarEstadoCiclo(int cicloId);
    void registrarDistribuicaoAtomica(int cicloId, String insumo, int totalDistribuido, Map<String, Integer> atendimentosPorUnidade);
    Map<String, Integer> consultarAtendimentos(int cicloId, String insumo);
}