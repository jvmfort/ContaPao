package contapao.servico;

import contapao.expedicao.StatusCiclo;
import contapao.persistencia.RepositorioReposicaoSQLite;
import contapao.rateio.PoliticaRateio;
import contapao.rateio.RateioIgualitario;
import contapao.rateio.RateioPorPrioridade;
import contapao.rateio.RateioProporcional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmacaoDistribuicaoServiceTest {

    private static final String FARINHA = "Farinha de Trigo 50kg";

    @TempDir
    Path pasta;

    private RepositorioReposicaoSQLite repositorio;
    private ConfirmacaoDistribuicaoService service;

    @BeforeEach
    void setUp() {
        repositorio = new RepositorioReposicaoSQLite(pasta.resolve("teste.db").toString());
        repositorio.inicializarBanco();
        repositorio.resetarDadosDemonstracao(); // saldo 100, ciclo 1 ENCERRADO
        service = new ConfirmacaoDistribuicaoService(repositorio, new RateioProporcional());
    }

    private Map<String, Integer> demandas(int a, int b, int c) {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("Padaria A", a);
        m.put("Padaria B", b);
        m.put("Padaria C", c);
        return m;
    }

    /** PoliticaRateio tem dois métodos, então não é interface funcional: precisa de classe anônima. */
    private PoliticaRateio politicaFixa(Map<String, Integer> resultado) {
        return new PoliticaRateio() {
            @Override
            public Map<String, Integer> calcularDistribuicao(int estoque, Map<String, Integer> demandas) {
                return resultado;
            }

            @Override
            public String getNomePolitica() {
                return "Política defeituosa (teste)";
            }
        };
    }

    private void assertNadaFoiGravado() {
        assertEquals(100, repositorio.consultarSaldoCentral(FARINHA));
        assertEquals(StatusCiclo.ENCERRADO, repositorio.consultarEstadoCiclo(1));
        assertTrue(repositorio.consultarAtendimentos(1, FARINHA).isEmpty());
    }

    // ---------------------------------------------------------- rateio (RN03 / RN04)

    @Test
    @DisplayName("RN03: atende 100% quando o estoque é igual à demanda")
    void atendeIntegralmenteNaFronteira() {
        Map<String, Integer> r = service.confirmar(1, FARINHA, demandas(50, 30, 20));
        assertEquals(Map.of("Padaria A", 50, "Padaria B", 30, "Padaria C", 20), r);
        assertEquals(0, repositorio.consultarSaldoCentral(FARINHA));
    }

    @Test
    @DisplayName("RN03: nunca entrega acima do pedido quando sobra estoque")
    void naoEntregaAcimaDoPedido() {
        Map<String, Integer> r = service.confirmar(1, FARINHA, demandas(15, 10, 5));
        assertEquals(Map.of("Padaria A", 15, "Padaria B", 10, "Padaria C", 5), r);
        assertEquals(70, repositorio.consultarSaldoCentral(FARINHA));
    }

    @Test
    @DisplayName("Proporcional: 70, 40, 40 com estoque 100 -> 48, 26, 26")
    void proporcional() {
        Map<String, Integer> r = service.confirmar(1, FARINHA, demandas(70, 40, 40));
        assertEquals(48, r.get("Padaria A"));
        assertEquals(26, r.get("Padaria B"));
        assertEquals(26, r.get("Padaria C"));
        assertEquals(0, repositorio.consultarSaldoCentral(FARINHA));
    }

    @Test
    @DisplayName("Proporcional: empate de demanda dá o resíduo à primeira unidade (5, 5, 5 com estoque 10 -> 4, 3, 3)")
    void desempatePelaOrdem() {
        assertTrue(repositorio.baixarSaldoCentral(FARINHA, 90)); // saldo 10
        Map<String, Integer> r = service.confirmar(1, FARINHA, demandas(5, 5, 5));
        assertEquals(Map.of("Padaria A", 4, "Padaria B", 3, "Padaria C", 3), r);
    }

    @Test
    @DisplayName("Prioridade: atende A -> B -> C (60, 50, 30 -> 60, 40, 0)")
    void prioridade() {
        service.trocarPolitica(new RateioPorPrioridade());
        Map<String, Integer> r = service.confirmar(1, FARINHA, demandas(60, 50, 30));
        assertEquals(Map.of("Padaria A", 60, "Padaria B", 40, "Padaria C", 0), r);
    }

    @Test
    @DisplayName("RN04: prioridade não depende do tipo do mapa recebido")
    void prioridadeIndependeDoMapa() {
        service.trocarPolitica(new RateioPorPrioridade());
        Map<String, Integer> embaralhado = new HashMap<>();
        embaralhado.put("Padaria C", 30);
        embaralhado.put("Padaria B", 50);
        embaralhado.put("Padaria A", 60);

        Map<String, Integer> r = service.confirmar(1, FARINHA, embaralhado);
        assertEquals(60, r.get("Padaria A"));
        assertEquals(40, r.get("Padaria B"));
        assertEquals(0, r.get("Padaria C"));
        assertEquals(List.of("Padaria A", "Padaria B", "Padaria C"), new ArrayList<>(r.keySet()));
    }

    @Test
    @DisplayName("RN04: unidades fora da lista padrão vão ao fim, em ordem alfabética")
    void unidadesForaDaListaPadrao() {
        service.trocarPolitica(new RateioPorPrioridade());
        Map<String, Integer> entrada = new LinkedHashMap<>();
        entrada.put("Padaria E", 10);
        entrada.put("Padaria D", 10);
        entrada.put("Padaria A", 10);

        Map<String, Integer> r = service.confirmar(1, FARINHA, entrada);
        assertEquals(List.of("Padaria A", "Padaria D", "Padaria E"), new ArrayList<>(r.keySet()));
    }

    @Test
    @DisplayName("Igualitário: 10, 80, 80 -> 10, 45, 45")
    void igualitario() {
        service.trocarPolitica(new RateioIgualitario());
        Map<String, Integer> r = service.confirmar(1, FARINHA, demandas(10, 80, 80));
        assertEquals(Map.of("Padaria A", 10, "Padaria B", 45, "Padaria C", 45), r);
    }

    // --------------------------------------------------------------- ciclo (RN02 / RN05)

    @Test
    @DisplayName("RN05: impede confirmação duplicada do mesmo ciclo")
    void impedeDuplicada() {
        service.confirmar(1, FARINHA, demandas(20, 20, 20));
        assertEquals(40, repositorio.consultarSaldoCentral(FARINHA));
        assertEquals(StatusCiclo.DISTRIBUIDO, repositorio.consultarEstadoCiclo(1));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.confirmar(1, FARINHA, demandas(20, 20, 20)));
        assertTrue(ex.getMessage().contains("já foi distribuído"));
        assertEquals(40, repositorio.consultarSaldoCentral(FARINHA));
    }

    @Test
    @DisplayName("RN02: ciclo ABERTO não distribui")
    void cicloAbertoNaoDistribui() {
        repositorio.criarCiclo(2); // nasce ABERTO
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.confirmar(2, FARINHA, demandas(10, 10, 10)));
        assertTrue(ex.getMessage().contains("ENCERRADO"));
        assertEquals(100, repositorio.consultarSaldoCentral(FARINHA));
    }

    @Test
    @DisplayName("RN02: ciclo inexistente")
    void cicloInexistente() {
        assertThrows(IllegalArgumentException.class, () -> service.confirmar(999, FARINHA, demandas(10, 10, 10)));
    }

    @Test
    @DisplayName("Insumo inexistente falha antes de qualquer escrita")
    void insumoInexistente() {
        assertThrows(IllegalArgumentException.class, () -> service.confirmar(1, "Insumo Fantasma", demandas(10, 10, 10)));
        assertNadaFoiGravado();
    }

    // ------------------------------------------------------------ construtor e política

    @Test
    void construtorRejeitaNulos() {
        assertThrows(NullPointerException.class, () -> new ConfirmacaoDistribuicaoService(null, new RateioProporcional()));
        assertThrows(NullPointerException.class, () -> new ConfirmacaoDistribuicaoService(repositorio, null));
    }

    @Test
    void trocarPoliticaFunciona() {
        assertThrows(NullPointerException.class, () -> service.trocarPolitica(null));
        RateioIgualitario nova = new RateioIgualitario();
        service.trocarPolitica(nova);
        assertSame(nova, service.getPoliticaRateio());
    }

    // ------------------------------------------------------------- validação de entrada

    @Test
    void rejeitaInsumoNuloOuEmBranco() {
        assertThrows(IllegalArgumentException.class, () -> service.confirmar(1, null, demandas(10, 10, 10)));
        assertThrows(IllegalArgumentException.class, () -> service.confirmar(1, "   ", demandas(10, 10, 10)));
        assertNadaFoiGravado();
    }

    @Test
    void rejeitaMapaNuloOuVazio() {
        assertThrows(IllegalArgumentException.class, () -> service.confirmar(1, FARINHA, null));
        assertThrows(IllegalArgumentException.class, () -> service.confirmar(1, FARINHA, Map.of()));
    }

    @Test
    void rejeitaValorNulo() {
        Map<String, Integer> comNulo = new LinkedHashMap<>();
        comNulo.put("Padaria A", null);
        assertThrows(IllegalArgumentException.class, () -> service.confirmar(1, FARINHA, comNulo));
    }

    @Test
    void rejeitaDemandaNegativa() {
        assertThrows(IllegalArgumentException.class, () -> service.confirmar(1, FARINHA, demandas(-5, 10, 10)));
        assertNadaFoiGravado();
    }

    // ------------------------------------------------------ invariantes (política defeituosa)

    private void assertInvariante(Map<String, Integer> resultadoDefeituoso, int a, int b, int c, String trecho) {
        service.trocarPolitica(politicaFixa(resultadoDefeituoso));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.confirmar(1, FARINHA, demandas(a, b, c)));
        assertTrue(ex.getMessage().contains(trecho), ex.getMessage());
        assertNadaFoiGravado();
    }

    @Test
    @DisplayName("Invariante: unidade que não pediu")
    void unidadeInexistente() {
        assertInvariante(Map.of("Padaria Fantasma", 10), 10, 10, 10, "atendimento inválido");
    }

    @Test
    @DisplayName("Invariante: quantidade negativa")
    void quantidadeNegativa() {
        assertInvariante(Map.of("Padaria A", -1), 10, 10, 10, "atendimento inválido");
    }

    @Test
    @DisplayName("Invariante: quantidade nula")
    void quantidadeNula() {
        Map<String, Integer> comNulo = new HashMap<>();
        comNulo.put("Padaria A", null);
        assertInvariante(comNulo, 10, 10, 10, "atendimento inválido");
    }

    @Test
    @DisplayName("Invariante RN03: acima do pedido")
    void acimaDoPedido() {
        assertInvariante(Map.of("Padaria A", 15), 10, 10, 10, "atendimento inválido");
    }

    @Test
    @DisplayName("Invariante RN03: soma maior que o estoque")
    void somaMaiorQueEstoque() {
        assertInvariante(Map.of("Padaria A", 60, "Padaria B", 60), 60, 60, 10, "excede o estoque");
    }

    @Test
    @DisplayName("Invariante: política que devolve null")
    void politicaDevolveNull() {
        assertInvariante(null, 10, 10, 10, "não devolveu resultado");
    }
}
