package contapao.expedicao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExpedicaoTest {

    private final CentralExpedicaoMatriz central = CentralExpedicaoMatriz.getInstance();

    @BeforeEach
    @AfterEach
    void limpar() {
        central.limparObservadores();
    }

    @Test
    void singleton() {
        assertSame(CentralExpedicaoMatriz.getInstance(), CentralExpedicaoMatriz.getInstance());
    }

    @Test
    void registrarRemoverEEvitarDuplicata() {
        PainelExpedicaoCentral painel = new PainelExpedicaoCentral();
        central.registrar(painel);
        central.registrar(painel);
        assertEquals(1, central.totalObservadores());
        central.remover(painel);
        assertEquals(0, central.totalObservadores());
        assertThrows(NullPointerException.class, () -> central.registrar(null));
    }

    @Test
    void observadorComDefeitoNaoDerrubaOsOutros() {
        List<String> chamadas = new ArrayList<>();
        ObservadorExpedicao defeituoso = new ObservadorExpedicao() {
            @Override
            public void aoEncerrarCiclo(int cicloId) {
                throw new IllegalStateException("falha");
            }

            @Override
            public void aoDistribuirCiclo(int cicloId, String insumo, Map<String, Integer> atendimentos) {
                throw new IllegalStateException("falha");
            }
        };
        ObservadorExpedicao saudavel = new ObservadorExpedicao() {
            @Override
            public void aoEncerrarCiclo(int cicloId) {
                chamadas.add("encerrou " + cicloId);
            }

            @Override
            public void aoDistribuirCiclo(int cicloId, String insumo, Map<String, Integer> atendimentos) {
                chamadas.add("distribuiu " + cicloId);
            }
        };
        central.registrar(defeituoso);
        central.registrar(saudavel);

        assertDoesNotThrow(() -> central.notificarEncerramento(7));
        assertDoesNotThrow(() -> central.notificarDistribuicao(7, "Farinha", Map.of("Padaria A", 1)));
        assertEquals(List.of("encerrou 7", "distribuiu 7"), chamadas);
    }

    @Test
    void painelRegistraTudo() {
        PainelExpedicaoCentral painel = new PainelExpedicaoCentral();
        painel.aoEncerrarCiclo(1);
        painel.aoDistribuirCiclo(1, "Farinha", Map.of("Padaria A", 10));
        assertEquals(2, painel.getHistorico().size());
        assertTrue(painel.getHistorico().get(1).contains("Padaria A=10"));
    }

    @Test
    void notificadorPainelCentralRegistraEventos() {
        NotificadorPainelCentral painel = new NotificadorPainelCentral();
        painel.aoEncerrarCiclo(3);
        painel.aoDistribuirCiclo(3, "Farinha", Map.of("Padaria A", 15));
        assertEquals(2, painel.getHistorico().size());
        assertTrue(painel.getHistorico().get(0).contains("Ciclo 3 encerrado"));
        assertTrue(painel.getHistorico().get(1).contains("Padaria A=15"));
    }

    @Test
    void gerenteSoVeSuaFilial() {
        GerenteFilial gerenteA = new GerenteFilial("Padaria A");
        gerenteA.aoDistribuirCiclo(1, "Farinha", Map.of("Padaria B", 20));
        assertTrue(gerenteA.getMensagens().isEmpty());

        gerenteA.aoDistribuirCiclo(1, "Farinha", Map.of("Padaria A", 30, "Padaria B", 20));
        gerenteA.aoEncerrarCiclo(2);
        assertEquals(2, gerenteA.getMensagens().size());
        assertTrue(gerenteA.getMensagens().get(0).contains("Padaria A recebeu 30"));
        assertEquals("Padaria A", gerenteA.getPadaria());
    }

    @Test
    void gerenteRejeitaPadariaVazia() {
        assertThrows(IllegalArgumentException.class, () -> new GerenteFilial(null));
        assertThrows(IllegalArgumentException.class, () -> new GerenteFilial("  "));
    }

    @Test
    void notificadorGerenteFilialFiltraEValida() {
        assertThrows(IllegalArgumentException.class, () -> new NotificadorGerenteFilial(null, "A"));
        assertThrows(IllegalArgumentException.class, () -> new NotificadorGerenteFilial(" ", "A"));
        assertThrows(IllegalArgumentException.class, () -> new NotificadorGerenteFilial("Carlos", null));
        assertThrows(IllegalArgumentException.class, () -> new NotificadorGerenteFilial("Carlos", "D"));

        NotificadorGerenteFilial notifA = new NotificadorGerenteFilial("Carlos", "A");
        assertEquals("Carlos", notifA.getNomeGerente());
        assertEquals("A", notifA.getCodigoPadaria());

        notifA.aoDistribuirCiclo(1, "Farinha", Map.of("Padaria B", 20));
        assertTrue(notifA.getMensagens().isEmpty());

        notifA.aoDistribuirCiclo(1, "Farinha", Map.of("Padaria A", 25, "Padaria B", 20));
        notifA.aoEncerrarCiclo(2);
        assertEquals(2, notifA.getMensagens().size());
        assertTrue(notifA.getMensagens().get(0).contains("Padaria A recebeu 25"));
    }

    @Test
    void statusCiclo() {
        assertFalse(StatusCiclo.ABERTO.podeDistribuir());
        assertTrue(StatusCiclo.ENCERRADO.podeDistribuir());
        assertFalse(StatusCiclo.DISTRIBUIDO.podeDistribuir());
    }
}