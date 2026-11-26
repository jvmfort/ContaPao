package contapao.fachada;

import contapao.catalogo.Insumo;
import contapao.catalogo.KitReposicao;
import contapao.expedicao.CentralExpedicaoMatriz;
import contapao.expedicao.GerenteFilial;
import contapao.expedicao.PainelExpedicaoCentral;
import contapao.expedicao.StatusCiclo;
import contapao.frete.CalculoFrete;
import contapao.frete.FreteFactory;
import contapao.frete.FreteRefrigeradoFactory;
import contapao.frete.FrotaPropriaFactory;
import contapao.frete.RetiradaMatrizFactory;
import contapao.integracao.AlertaSonoro;
import contapao.integracao.AlertaSonoroNulo;
import contapao.persistencia.RepositorioReposicaoSQLite;
import contapao.rateio.RateioPorPrioridade;
import contapao.rateio.RateioProporcional;
import contapao.servico.FinanceiroInternoService;
import contapao.servico.GuiaRemessaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReposicaoPadariaFacadeTest {

    private static final String FARINHA = "Farinha de Trigo 50kg";

    @TempDir
    Path pasta;

    private RepositorioReposicaoSQLite repositorio;
    private FinanceiroInternoService financeiro;
    private GuiaRemessaService guias;
    private ReposicaoPadariaFacade fachada;

    @BeforeEach
    void setUp() {
        CentralExpedicaoMatriz.getInstance().limparObservadores();
        repositorio = new RepositorioReposicaoSQLite(pasta.resolve("teste.db").toString());
        repositorio.inicializarBanco();
        repositorio.resetarDadosDemonstracao(); // Farinha 100, ciclo 1 ENCERRADO
        repositorio.cadastrarEstoque("Fermento Biologico Seco", 200);
        repositorio.cadastrarEstoque("Acucar Cristal", 300);
        financeiro = new FinanceiroInternoService();
        guias = new GuiaRemessaService();
        fachada = new ReposicaoPadariaFacade(repositorio, new RateioProporcional(), financeiro, guias, null);
    }

    @AfterEach
    void tearDown() {
        CentralExpedicaoMatriz.getInstance().limparObservadores();
    }

    private Map<String, Integer> demandas(int a, int b, int c) {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("Padaria A", a);
        m.put("Padaria B", b);
        m.put("Padaria C", c);
        return m;
    }

    // ------------------------------------------------------------------ construção

    @Test
    void construtoresRejeitamNulos() {
        assertThrows(NullPointerException.class, () -> new ReposicaoPadariaFacade(null, new RateioProporcional()));
        assertThrows(NullPointerException.class, () -> new ReposicaoPadariaFacade(repositorio, null));
        assertThrows(NullPointerException.class, () ->
                new ReposicaoPadariaFacade(repositorio, new RateioProporcional(), null, guias, null));
        assertThrows(NullPointerException.class, () ->
                new ReposicaoPadariaFacade(repositorio, new RateioProporcional(), financeiro, null, null));
    }

    @Test
    @DisplayName("Construtor simples cria serviços padrão e AlertaSonoroNulo")
    void construtorSimples() {
        ReposicaoPadariaFacade f = new ReposicaoPadariaFacade(repositorio, new RateioProporcional());
        assertNotNull(f.getFinanceiroService());
        assertNotNull(f.getGuiaService());
        assertNotNull(f.getEstoqueService());
        assertInstanceOf(AlertaSonoroNulo.class, f.getAlertaSonoro());
    }

    @Test
    @DisplayName("setAlertaSonoro(null) volta para AlertaSonoroNulo")
    void alertaNuloVoltaParaNullObject() {
        fachada.setAlertaSonoro(msg -> { });
        fachada.setAlertaSonoro(null);
        assertInstanceOf(AlertaSonoroNulo.class, fachada.getAlertaSonoro());
    }

    @Test
    void trocarPoliticaRejeitaNulo() {
        assertThrows(NullPointerException.class, () -> fachada.trocarPolitica(null));
    }

    // ---------------------------------------------------- confirmarDistribuicao (RN01)

    @Test
    @DisplayName("RN01: GERENTE não confirma distribuição e nada é gravado")
    void gerenteBloqueado() {
        assertThrows(SecurityException.class, () ->
                fachada.confirmarDistribuicao(PerfilAcesso.GERENTE, 1, FARINHA, demandas(20, 20, 20)));
        assertEquals(100, repositorio.consultarSaldoCentral(FARINHA));
        assertEquals(StatusCiclo.ENCERRADO, repositorio.consultarEstadoCiclo(1));
    }

    @Test
    @DisplayName("Perfil nulo também é bloqueado")
    void perfilNuloBloqueado() {
        assertThrows(SecurityException.class, () ->
                fachada.confirmarDistribuicao(null, 1, FARINHA, demandas(20, 20, 20)));
    }

    @Test
    @DisplayName("CONTADOR distribui, o banco reflete e a política pode ser trocada em tempo de execução")
    void contadorDistribui() {
        Map<String, Integer> r = fachada.confirmarDistribuicao(PerfilAcesso.CONTADOR, 1, FARINHA, demandas(70, 40, 40));
        assertEquals(Map.of("Padaria A", 48, "Padaria B", 26, "Padaria C", 26), r);
        assertEquals(0, repositorio.consultarSaldoCentral(FARINHA));
        assertEquals(StatusCiclo.DISTRIBUIDO, repositorio.consultarEstadoCiclo(1));

        fachada.abrirCiclo(2);
        fachada.encerrarCiclo(2);
        repositorio.cadastrarEstoque(FARINHA, 100);
        fachada.trocarPolitica(new RateioPorPrioridade());
        Map<String, Integer> r2 = fachada.confirmarDistribuicao(PerfilAcesso.CONTADOR, 2, FARINHA, demandas(60, 50, 30));
        assertEquals(Map.of("Padaria A", 60, "Padaria B", 40, "Padaria C", 0), r2);
    }

    @Test
    @DisplayName("Persistência real: nova instância do repositório enxerga a distribuição")
    void persistenciaReal() {
        fachada.confirmarDistribuicao(PerfilAcesso.CONTADOR, 1, FARINHA, demandas(30, 30, 20));
        RepositorioReposicaoSQLite novo = new RepositorioReposicaoSQLite(pasta.resolve("teste.db").toString());
        assertEquals(20, novo.consultarSaldoCentral(FARINHA));
        assertEquals(StatusCiclo.DISTRIBUIDO, novo.consultarEstadoCiclo(1));
        assertEquals(30, novo.consultarAtendimentos(1, FARINHA).get("Padaria A"));
    }

    // ------------------------------------------------------ ciclo e observadores

    @Test
    @DisplayName("Observer: painel e gerente são avisados no encerramento e na distribuição")
    void observadoresSaoNotificados() {
        PainelExpedicaoCentral painel = new PainelExpedicaoCentral();
        GerenteFilial gerenteB = new GerenteFilial("Padaria B");
        CentralExpedicaoMatriz.getInstance().registrar(painel);
        CentralExpedicaoMatriz.getInstance().registrar(gerenteB);

        fachada.abrirCiclo(2);
        fachada.encerrarCiclo(2);
        fachada.confirmarDistribuicao(PerfilAcesso.CONTADOR, 1, FARINHA, demandas(70, 40, 40));

        assertEquals(2, painel.getHistorico().size());
        assertTrue(painel.getHistorico().get(0).contains("Ciclo 2 encerrado"));
        assertTrue(painel.getHistorico().get(1).contains("Ciclo 1 distribuído"));
        assertEquals(2, gerenteB.getMensagens().size());
        assertTrue(gerenteB.getMensagens().get(1).contains("Padaria B recebeu 26"));
    }

    @Test
    @DisplayName("Distribuição recusada não notifica ninguém")
    void recusaNaoNotifica() {
        PainelExpedicaoCentral painel = new PainelExpedicaoCentral();
        CentralExpedicaoMatriz.getInstance().registrar(painel);
        assertThrows(SecurityException.class, () ->
                fachada.confirmarDistribuicao(PerfilAcesso.GERENTE, 1, FARINHA, demandas(10, 10, 10)));
        assertTrue(painel.getHistorico().isEmpty());
    }

    @Test
    void encerrarCicloInvalidoLancaErro() {
        assertThrows(IllegalStateException.class, () -> fachada.encerrarCiclo(1)); // já ENCERRADO
        assertThrows(IllegalArgumentException.class, () -> fachada.encerrarCiclo(999));
        fachada.abrirCiclo(5);
        assertThrows(IllegalArgumentException.class, () -> fachada.abrirCiclo(5)); // duplicado
    }

    // ------------------------------------------------- confirmarDistribuicaoEExpedir

    @Test
    @DisplayName("Distribui e expede: alerta, débito e guia por padaria atendida; padaria com zero é ignorada")
    void distribuiEExpede() {
        List<String> alertas = new ArrayList<>();
        fachada.setAlertaSonoro(alertas::add);
        fachada.trocarPolitica(new RateioPorPrioridade());

        KitReposicao kit = new KitReposicao("Kit");
        kit.adicionar(new Insumo("Farinha", 4.0));
        kit.adicionar(new Insumo("Fermento", 1.0)); // R$ 5,00 por unidade

        Map<String, Integer> r = fachada.confirmarDistribuicaoEExpedir(
                PerfilAcesso.CONTADOR, 1, FARINHA, kit, new FrotaPropriaFactory(), demandas(60, 50, 30));

        assertEquals(Map.of("Padaria A", 60, "Padaria B", 40, "Padaria C", 0), r);
        assertEquals(2, alertas.size());              // C ficou com 0: nada a expedir
        assertEquals(2, guias.getGuiasEmitidas().size());
        assertEquals(60 * 5.0 + 15.0, financeiro.totalDebitado("Padaria A"), 0.0001);
        assertEquals(40 * 5.0 + 15.0, financeiro.totalDebitado("Padaria B"), 0.0001);
        assertEquals(0.0, financeiro.totalDebitado("Padaria C"), 0.0001);
        assertEquals(0, repositorio.consultarSaldoCentral(FARINHA)); // baixado uma única vez, pela distribuição
    }

    @Test
    @DisplayName("Distribui e expede: GERENTE bloqueado e argumentos nulos rejeitados antes de gravar")
    void distribuiEExpedeValidacoes() {
        Insumo item = new Insumo("Farinha", 1.0);
        assertThrows(SecurityException.class, () -> fachada.confirmarDistribuicaoEExpedir(
                PerfilAcesso.GERENTE, 1, FARINHA, item, new FrotaPropriaFactory(), demandas(10, 10, 10)));
        assertThrows(NullPointerException.class, () -> fachada.confirmarDistribuicaoEExpedir(
                PerfilAcesso.CONTADOR, 1, FARINHA, null, new FrotaPropriaFactory(), demandas(10, 10, 10)));
        assertThrows(NullPointerException.class, () -> fachada.confirmarDistribuicaoEExpedir(
                PerfilAcesso.CONTADOR, 1, FARINHA, item, null, demandas(10, 10, 10)));
        assertEquals(StatusCiclo.ENCERRADO, repositorio.consultarEstadoCiclo(1));
    }

    @Test
    @DisplayName("Distribui e expede: débito recusado depois da distribuição lança IllegalStateException")
    void distribuiEExpedeDebitoRecusado() {
        FreteFactory freteNegativo = new FreteFactory() {
            @Override
            public CalculoFrete criarFrete() {
                return () -> -1000.0;
            }
        };
        assertThrows(IllegalStateException.class, () -> fachada.confirmarDistribuicaoEExpedir(
                PerfilAcesso.CONTADOR, 1, FARINHA, new Insumo("Farinha", 1.0), freteNegativo, demandas(10, 10, 10)));
    }

    // --------------------------------------------------- processarReposicao (RF05)

    @Test
    @DisplayName("Reposição avulsa com sucesso: baixa estoque, debita, emite alerta e guia")
    void reposicaoComSucesso() {
        List<String> alertas = new ArrayList<>();
        AlertaSonoro espiao = alertas::add;
        fachada.setAlertaSonoro(espiao);

        boolean ok = fachada.processarReposicao("PAD-A", FARINHA, 50, 40, new Insumo("Farinha", 1.5), new FrotaPropriaFactory());

        assertTrue(ok);
        assertEquals(60, repositorio.consultarSaldoCentral(FARINHA));
        assertEquals(40 * 1.5 + 15.0, financeiro.totalDebitado("PAD-A"), 0.0001);
        assertEquals(1, alertas.size());
        assertTrue(alertas.get(0).contains("PAD-A"));
        assertTrue(alertas.get(0).contains("40x " + FARINHA));
        assertEquals(1, guias.getGuiasEmitidas().size());
    }

    @Test
    @DisplayName("Reposição avulsa com kit e com as três modalidades de frete")
    void reposicaoComKitEFretes() {
        KitReposicao kit = new KitReposicao("Kit Padaria");
        kit.adicionar(new Insumo("Fermento", 30.0));
        kit.adicionar(new Insumo("Acucar", 20.0));

        assertTrue(fachada.processarReposicao("PAD-B", "Fermento Biologico Seco", 20, 20, kit, new FreteRefrigeradoFactory()));
        assertTrue(fachada.processarReposicao("PAD-C", "Acucar Cristal", 10, 10, kit, new RetiradaMatrizFactory()));

        assertEquals(180, repositorio.consultarSaldoCentral("Fermento Biologico Seco"));
        assertEquals(290, repositorio.consultarSaldoCentral("Acucar Cristal"));
        assertEquals(20 * 50.0 + 45.0, financeiro.totalDebitado("PAD-B"), 0.0001);
        assertEquals(10 * 50.0, financeiro.totalDebitado("PAD-C"), 0.0001);
    }

    @Test
    @DisplayName("RN03: atendido maior que solicitado é recusado sem baixar estoque")
    void reposicaoAtendidoMaiorQueSolicitado() {
        assertFalse(fachada.processarReposicao("PAD-A", FARINHA, 10, 15, new Insumo("F", 1.0), new RetiradaMatrizFactory()));
        assertEquals(100, repositorio.consultarSaldoCentral(FARINHA));
    }

    @Test
    @DisplayName("RN03: saldo insuficiente é recusado sem baixar estoque")
    void reposicaoSaldoInsuficiente() {
        assertFalse(fachada.processarReposicao("PAD-A", FARINHA, 600, 600, new Insumo("F", 1.0), new RetiradaMatrizFactory()));
        assertEquals(100, repositorio.consultarSaldoCentral(FARINHA));
    }

    @Test
    @DisplayName("Quantidades zero ou negativas são recusadas")
    void reposicaoQuantidadesInvalidas() {
        Insumo f = new Insumo("F", 1.0);
        assertFalse(fachada.processarReposicao("PAD-A", FARINHA, 0, 10, f, new RetiradaMatrizFactory()));
        assertFalse(fachada.processarReposicao("PAD-A", FARINHA, 10, 0, f, new RetiradaMatrizFactory()));
        assertFalse(fachada.processarReposicao("PAD-A", FARINHA, -5, 5, f, new RetiradaMatrizFactory()));
        assertEquals(100, repositorio.consultarSaldoCentral(FARINHA));
    }

    @Test
    @DisplayName("Débito recusado devolve o estoque que tinha sido baixado")
    void reposicaoDebitoRecusadoDevolveEstoque() {
        FreteFactory freteNegativo = new FreteFactory() {
            @Override
            public CalculoFrete criarFrete() {
                return () -> -1000.0;
            }
        };
        assertFalse(fachada.processarReposicao("PAD-A", FARINHA, 10, 10, new Insumo("F", 1.0), freteNegativo));
        assertEquals(100, repositorio.consultarSaldoCentral(FARINHA));
        assertTrue(guias.getGuiasEmitidas().isEmpty());
    }

    @Test
    void reposicaoRejeitaNulosEInsumoInexistente() {
        assertThrows(NullPointerException.class, () ->
                fachada.processarReposicao("PAD-A", FARINHA, 10, 10, null, new RetiradaMatrizFactory()));
        assertThrows(NullPointerException.class, () ->
                fachada.processarReposicao("PAD-A", FARINHA, 10, 10, new Insumo("F", 1.0), null));
        assertThrows(IllegalArgumentException.class, () ->
                fachada.processarReposicao("PAD-A", "Insumo Fantasma", 10, 10, new Insumo("F", 1.0), new RetiradaMatrizFactory()));
    }

    @Test
    @DisplayName("Estoque único: reposição avulsa e distribuição de ciclo enxergam o mesmo saldo")
    void estoqueUnico() {
        assertTrue(fachada.processarReposicao("PAD-A", FARINHA, 30, 30, new Insumo("F", 1.0), new RetiradaMatrizFactory()));
        Map<String, Integer> r = fachada.confirmarDistribuicao(PerfilAcesso.CONTADOR, 1, FARINHA, demandas(50, 50, 50));
        assertEquals(70, r.values().stream().mapToInt(Integer::intValue).sum()); // só havia 70 depois da reposição
        assertEquals(0, repositorio.consultarSaldoCentral(FARINHA));
    }
}
