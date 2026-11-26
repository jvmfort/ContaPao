package contapao.servico;

import contapao.persistencia.RepositorioReposicaoSQLite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ServicosTest {

    @TempDir
    Path pasta;

    private EstoqueMatrizService estoque;

    @BeforeEach
    void setUp() {
        RepositorioReposicaoSQLite repositorio = new RepositorioReposicaoSQLite(pasta.resolve("t.db").toString());
        repositorio.inicializarBanco();
        repositorio.resetarDadosDemonstracao();
        estoque = new EstoqueMatrizService(repositorio);
    }

    // ------------------------------------------------------------ EstoqueMatrizService

    @Test
    void estoqueRejeitaRepositorioNulo() {
        assertThrows(NullPointerException.class, () -> new EstoqueMatrizService(null));
    }

    @Test
    @DisplayName("Estoque: cadastrar soma ao saldo, consultar e baixar")
    void cadastrarConsultarBaixar() {
        estoque.cadastrarEstoque("Sal Refinado", 40);
        estoque.cadastrarEstoque("Sal Refinado", 10);
        assertEquals(50, estoque.consultarSaldo("Sal Refinado"));
        assertTrue(estoque.baixarEstoque("Sal Refinado", 15));
        assertEquals(35, estoque.consultarSaldo("Sal Refinado"));
        assertFalse(estoque.baixarEstoque("Sal Refinado", 36));
        assertFalse(estoque.baixarEstoque("Sal Refinado", 0));
        assertFalse(estoque.baixarEstoque("Insumo Fantasma", 1));
        assertEquals(35, estoque.consultarSaldo("Sal Refinado"));
    }

    @Test
    void estoqueRejeitaCadastroInvalido() {
        assertThrows(IllegalArgumentException.class, () -> estoque.cadastrarEstoque(null, 10));
        assertThrows(IllegalArgumentException.class, () -> estoque.cadastrarEstoque(" ", 10));
        assertThrows(IllegalArgumentException.class, () -> estoque.cadastrarEstoque("Sal", 0));
        assertThrows(IllegalArgumentException.class, () -> estoque.cadastrarEstoque("Sal", -1));
    }

    @Test
    void consultarSaldoDeInsumoInexistenteLancaErro() {
        assertThrows(IllegalArgumentException.class, () -> estoque.consultarSaldo("Insumo Fantasma"));
    }

    @Test
    @DisplayName("RN03: validarDisponibilidade aceita zero e recusa negativo, acima do pedido e acima do saldo")
    void validarDisponibilidade() {
        String f = "Farinha de Trigo 50kg"; // saldo 100
        assertTrue(estoque.validarDisponibilidade(f, 50, 50));
        assertTrue(estoque.validarDisponibilidade(f, 50, 0));
        assertTrue(estoque.validarDisponibilidade(f, 200, 100));
        assertFalse(estoque.validarDisponibilidade(f, 50, 51));
        assertFalse(estoque.validarDisponibilidade(f, 200, 101));
        assertFalse(estoque.validarDisponibilidade(f, -1, 0));
        assertFalse(estoque.validarDisponibilidade(f, 10, -1));
    }

    // ------------------------------------------------------ FinanceiroInternoService

    @Test
    void financeiroAcumulaDebitosPorFilial() {
        FinanceiroInternoService fin = new FinanceiroInternoService();
        assertTrue(fin.debitarContaFilial("PAD-A", 100.0));
        assertTrue(fin.debitarContaFilial("PAD-A", 50.5));
        assertTrue(fin.debitarContaFilial("PAD-B", 0.0));
        assertEquals(150.5, fin.totalDebitado("PAD-A"), 0.0001);
        assertEquals(0.0, fin.totalDebitado("PAD-B"), 0.0001);
        assertEquals(0.0, fin.totalDebitado("PAD-Z"), 0.0001);
    }

    @Test
    void financeiroRecusaValoresInvalidos() {
        FinanceiroInternoService fin = new FinanceiroInternoService();
        assertFalse(fin.debitarContaFilial("PAD-A", -0.01));
        assertFalse(fin.debitarContaFilial("PAD-A", Double.NaN));
        assertFalse(fin.debitarContaFilial("PAD-A", Double.POSITIVE_INFINITY));
        assertFalse(fin.debitarContaFilial(null, 10.0));
        assertFalse(fin.debitarContaFilial("  ", 10.0));
        assertEquals(0.0, fin.totalDebitado("PAD-A"), 0.0001);
    }

    // -------------------------------------------------------- GuiaRemessaService

    @Test
    void guiaEmiteCodigosSequenciaisEFormataEmPtBr() {
        GuiaRemessaService svc = new GuiaRemessaService();
        GuiaRemessa g1 = svc.gerarGuia("Padaria A", "48x Farinha", 1234.5);
        GuiaRemessa g2 = svc.gerarGuia("Padaria B", "26x Farinha", 0.0);

        assertEquals("REM-0001", g1.codigo());
        assertEquals("REM-0002", g2.codigo());
        assertEquals(2, svc.getGuiasEmitidas().size());
        assertEquals("GUIA DE REMESSA REM-0001\nDestino: Filial Padaria A\nItens: 48x Farinha\nCusto total: R$ 1234,50",
                g1.formatar());
        assertThrows(UnsupportedOperationException.class, () -> svc.getGuiasEmitidas().clear());
    }

    @Test
    void guiaRejeitaDadosInvalidos() {
        GuiaRemessaService svc = new GuiaRemessaService();
        assertThrows(IllegalArgumentException.class, () -> svc.gerarGuia(null, "1x A", 1.0));
        assertThrows(IllegalArgumentException.class, () -> svc.gerarGuia(" ", "1x A", 1.0));
        assertThrows(IllegalArgumentException.class, () -> svc.gerarGuia("PAD-A", null, 1.0));
        assertThrows(IllegalArgumentException.class, () -> svc.gerarGuia("PAD-A", " ", 1.0));
        assertThrows(IllegalArgumentException.class, () -> svc.gerarGuia("PAD-A", "1x A", -1.0));
        assertTrue(svc.getGuiasEmitidas().isEmpty());
    }
}
