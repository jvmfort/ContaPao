package contapao.rateio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PoliticasRateioTest {

    private Map<String, Integer> demandas(int a, int b, int c) {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("Padaria A", a);
        m.put("Padaria B", b);
        m.put("Padaria C", c);
        return m;
    }

    @Test
    @DisplayName("Todas as políticas rejeitam estoque negativo")
    void deveRejeitarEstoqueNegativo() {
        Map<String, Integer> d = demandas(10, 10, 10);
        assertThrows(IllegalArgumentException.class, () -> new RateioProporcional().calcularDistribuicao(-1, d));
        assertThrows(IllegalArgumentException.class, () -> new RateioPorPrioridade().calcularDistribuicao(-1, d));
        assertThrows(IllegalArgumentException.class, () -> new RateioIgualitario().calcularDistribuicao(-1, d));
    }

    @Test
    @DisplayName("Proporcional: com demanda vazia retorna mapa vazio")
    void proporcionalComDemandaVazia() {
        Map<String, Integer> r = new RateioProporcional().calcularDistribuicao(50, Map.of());
        assertTrue(r.isEmpty());
    }

    @Test
    @DisplayName("Proporcional: sobra de estoque não excede pedidos")
    void proporcionalComSobraDeEstoque() {
        Map<String, Integer> r = new RateioProporcional().calcularDistribuicao(200, demandas(10, 20, 30));
        assertEquals(Map.of("Padaria A", 10, "Padaria B", 20, "Padaria C", 30), r);
    }

    @Test
    @DisplayName("PorPrioridade: atende sequencialmente na ordem de inserção")
    void prioridadeSequencial() {
        Map<String, Integer> r = new RateioPorPrioridade().calcularDistribuicao(70, demandas(50, 40, 30));
        assertEquals(50, r.get("Padaria A"));
        assertEquals(20, r.get("Padaria B"));
        assertEquals(0, r.get("Padaria C"));
    }

    @Test
    @DisplayName("Igualitário: respeita teto menor e divide restante igualmente")
    void igualitarioComTeto() {
        Map<String, Integer> r = new RateioIgualitario().calcularDistribuicao(100, demandas(10, 80, 80));
        assertEquals(10, r.get("Padaria A"));
        assertEquals(45, r.get("Padaria B"));
        assertEquals(45, r.get("Padaria C"));
    }

    @Test
    @DisplayName("Políticas possuem nomes não vazios")
    void politicasPossuemNome() {
        assertFalse(new RateioProporcional().getNomePolitica().isBlank());
        assertFalse(new RateioPorPrioridade().getNomePolitica().isBlank());
        assertFalse(new RateioIgualitario().getNomePolitica().isBlank());
    }
}
