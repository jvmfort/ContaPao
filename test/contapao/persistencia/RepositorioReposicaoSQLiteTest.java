package contapao.persistencia;

import contapao.expedicao.StatusCiclo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RepositorioReposicaoSQLiteTest {

    private static final String FARINHA = "Farinha de Trigo 50kg";

    @TempDir
    Path pasta;

    private RepositorioReposicaoSQLite repo;

    @BeforeEach
    void setUp() {
        repo = new RepositorioReposicaoSQLite(pasta.resolve("t.db").toString());
        repo.inicializarBanco();
        repo.resetarDadosDemonstracao(); // Farinha 100, ciclo 1 ENCERRADO
    }

    @Test
    @DisplayName("RN06 Rollback: saldo insuficiente desfaz também a mudança de estado do ciclo")
    void rollbackQuandoSaldoNaoCobre() {
        assertThrows(IllegalStateException.class, () ->
                repo.registrarDistribuicaoAtomica(1, FARINHA, 150, Map.of("Padaria A", 150)));
        assertEquals(100, repo.consultarSaldoCentral(FARINHA));
        assertEquals(StatusCiclo.ENCERRADO, repo.consultarEstadoCiclo(1));
        assertTrue(repo.consultarAtendimentos(1, FARINHA).isEmpty());
    }

    @Test
    @DisplayName("RN06 Rollback: falha ao gravar atendimentos desfaz saldo e estado")
    void rollbackQuandoInsertFalha() {
        Map<String, Integer> comNulo = new LinkedHashMap<>();
        comNulo.put("Padaria A", null); // unboxing de null estoura no meio da transação
        assertThrows(NullPointerException.class, () ->
                repo.registrarDistribuicaoAtomica(1, FARINHA, 10, comNulo));
        assertEquals(100, repo.consultarSaldoCentral(FARINHA));
        assertEquals(StatusCiclo.ENCERRADO, repo.consultarEstadoCiclo(1));
    }

    @Test
    @DisplayName("RN05 no banco: a segunda gravação do mesmo ciclo é recusada pelo próprio SQL")
    void bancoRecusaSegundaDistribuicao() {
        repo.registrarDistribuicaoAtomica(1, FARINHA, 30, Map.of("Padaria A", 30));
        assertThrows(IllegalStateException.class, () ->
                repo.registrarDistribuicaoAtomica(1, FARINHA, 30, Map.of("Padaria A", 30)));
        assertEquals(70, repo.consultarSaldoCentral(FARINHA));
    }

    @Test
    void insumoInexistenteNaDistribuicaoFazRollback() {
        assertThrows(IllegalStateException.class, () ->
                repo.registrarDistribuicaoAtomica(1, "Fantasma", 0, Map.of()));
        assertEquals(StatusCiclo.ENCERRADO, repo.consultarEstadoCiclo(1));
    }

    @Test
    void totalNegativoERejeitado() {
        assertThrows(IllegalArgumentException.class, () ->
                repo.registrarDistribuicaoAtomica(1, FARINHA, -1, Map.of()));
    }

    @Test
    @DisplayName("Atendimentos voltam na ordem em que foram gravados")
    void atendimentosPreservamOrdem() {
        Map<String, Integer> atendimentos = new LinkedHashMap<>();
        atendimentos.put("Padaria C", 5);
        atendimentos.put("Padaria A", 10);
        atendimentos.put("Padaria B", 7);
        repo.registrarDistribuicaoAtomica(1, FARINHA, 22, atendimentos);

        List<String> ordem = new ArrayList<>(repo.consultarAtendimentos(1, FARINHA).keySet());
        assertEquals(List.of("Padaria C", "Padaria A", "Padaria B"), ordem);
        assertEquals(78, repo.consultarSaldoCentral(FARINHA));
    }

    @Test
    void cicloCriarEncerrarEConsultar() {
        repo.criarCiclo(2);
        assertEquals(StatusCiclo.ABERTO, repo.consultarEstadoCiclo(2));
        repo.encerrarCiclo(2);
        assertEquals(StatusCiclo.ENCERRADO, repo.consultarEstadoCiclo(2));

        assertThrows(IllegalArgumentException.class, () -> repo.criarCiclo(2));
        assertThrows(IllegalStateException.class, () -> repo.encerrarCiclo(2));
        assertThrows(IllegalArgumentException.class, () -> repo.encerrarCiclo(404));
        assertThrows(IllegalArgumentException.class, () -> repo.consultarEstadoCiclo(404));
    }

    @Test
    void baixarSaldoCentral() {
        assertTrue(repo.baixarSaldoCentral(FARINHA, 100));
        assertFalse(repo.baixarSaldoCentral(FARINHA, 1));
        assertFalse(repo.baixarSaldoCentral("Fantasma", 1));
        assertThrows(IllegalArgumentException.class, () -> repo.baixarSaldoCentral(FARINHA, -1));
        assertEquals(0, repo.consultarSaldoCentral(FARINHA));
    }

    @Test
    void cadastrarEstoqueCriaESoma() {
        repo.cadastrarEstoque("Sal", 5);
        repo.cadastrarEstoque("Sal", 5);
        assertEquals(10, repo.consultarSaldoCentral("Sal"));
    }

    @Test
    void consultarSaldoInexistente() {
        assertThrows(IllegalArgumentException.class, () -> repo.consultarSaldoCentral("Fantasma"));
    }

    @Test
    @DisplayName("Banco inacessível vira RuntimeException com mensagem clara")
    void bancoInacessivel() {
        RepositorioReposicaoSQLite ruim = new RepositorioReposicaoSQLite(pasta.resolve("nao/existe/x.db").toString());
        assertThrows(RuntimeException.class, ruim::inicializarBanco);
        assertThrows(RuntimeException.class, ruim::resetarDadosDemonstracao);
        assertThrows(RuntimeException.class, () -> ruim.consultarSaldoCentral(FARINHA));
        assertThrows(RuntimeException.class, () -> ruim.consultarEstadoCiclo(1));
        assertThrows(RuntimeException.class, () -> ruim.consultarAtendimentos(1, FARINHA));
        assertThrows(RuntimeException.class, () -> ruim.cadastrarEstoque(FARINHA, 1));
        assertThrows(RuntimeException.class, () -> ruim.baixarSaldoCentral(FARINHA, 1));
        assertThrows(RuntimeException.class, () -> ruim.criarCiclo(9));
        assertThrows(RuntimeException.class, () -> ruim.encerrarCiclo(1));
        assertThrows(RuntimeException.class, () -> ruim.registrarDistribuicaoAtomica(1, FARINHA, 1, Map.of()));
    }
}
