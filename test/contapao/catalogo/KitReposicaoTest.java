package contapao.catalogo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KitReposicaoTest {

    @Test
    @DisplayName("Insumo rejeita custo negativo e nome nulo")
    void insumoValidaConstrutor() {
        assertThrows(IllegalArgumentException.class, () -> new Insumo("Farinha", -0.01));
        assertThrows(NullPointerException.class, () -> new Insumo(null, 10.0));
    }

    @Test
    @DisplayName("KitReposicao calcula custo recursivamente com sub-kits")
    void kitCalculaCustoRecursivo() {
        Insumo farinha = new Insumo("Farinha", 100.0);
        Insumo fermento = new Insumo("Fermento", 25.0);
        Insumo acucar = new Insumo("Acucar", 15.0);

        KitReposicao subKit = new KitReposicao("SubKit Doce");
        subKit.adicionar(fermento);
        subKit.adicionar(acucar);

        KitReposicao kitPrincipal = new KitReposicao("Kit Completo");
        kitPrincipal.adicionar(farinha);
        kitPrincipal.adicionar(subKit);

        assertEquals(140.0, kitPrincipal.calcularCusto(), 0.0001);
        assertEquals("Kit Completo", kitPrincipal.getNome());
        assertEquals(2, kitPrincipal.getItens().size());

        kitPrincipal.remover(subKit);
        assertEquals(100.0, kitPrincipal.calcularCusto(), 0.0001);
    }

    @Test
    @DisplayName("KitReposicao impede auto-referência e item nulo")
    void kitImpedeAutoReferenciaENulo() {
        KitReposicao kit = new KitReposicao("Kit");
        assertThrows(IllegalArgumentException.class, () -> kit.adicionar(kit));
        assertThrows(NullPointerException.class, () -> kit.adicionar(null));
        assertThrows(NullPointerException.class, () -> new KitReposicao(null));
    }
}
