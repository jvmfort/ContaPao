package contapao.integracao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class IntegracaoTest {

    @Test
    @DisplayName("Adapter: converte quantidade para double e interpreta a resposta do moinho legado")
    void adapterConfirmaPedido() {
        FornecedorInsumo fornecedor = new MoinhoLegadoAdapter(new ApiMoinhoLegado());
        assertTrue(fornecedor.solicitarInsumo("Farinha Especial", 10));
    }

    @Test
    void adapterRecusaQuantidadeInvalida() {
        FornecedorInsumo fornecedor = new MoinhoLegadoAdapter(new ApiMoinhoLegado());
        assertFalse(fornecedor.solicitarInsumo("Farinha Especial", 0));
        assertFalse(fornecedor.solicitarInsumo("Farinha Especial", -3));
    }

    @Test
    void adapterTrataRespostaNegativaOuNula() {
        ApiMoinhoLegado recusa = new ApiMoinhoLegado() {
            @Override
            public String enviarPedidoFarinha(String tipoFarinha, double quantidadeSacos) {
                return "NEGADO";
            }
        };
        ApiMoinhoLegado nula = new ApiMoinhoLegado() {
            @Override
            public String enviarPedidoFarinha(String tipoFarinha, double quantidadeSacos) {
                return null;
            }
        };
        assertFalse(new MoinhoLegadoAdapter(recusa).solicitarInsumo("Farinha", 5));
        assertFalse(new MoinhoLegadoAdapter(nula).solicitarInsumo("Farinha", 5));
    }

    @Test
    @DisplayName("Null Object: AlertaSonoroNulo não faz nada e não lança exceção")
    void alertaNuloNaoFazNada() {
        PrintStream original = System.out;
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        System.setOut(new PrintStream(saida, true, StandardCharsets.UTF_8));
        try {
            assertDoesNotThrow(() -> new AlertaSonoroNulo().emitirAlerta("qualquer coisa"));
        } finally {
            System.setOut(original);
        }
        assertEquals("", saida.toString(StandardCharsets.UTF_8));
    }

    @Test
    void sireneRealEscreveOAlerta() {
        PrintStream original = System.out;
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        System.setOut(new PrintStream(saida, true, StandardCharsets.UTF_8));
        try {
            new AlertaSireneReal().emitirAlerta("Expedicao liberada");
        } finally {
            System.setOut(original);
        }
        assertTrue(saida.toString(StandardCharsets.UTF_8).contains("Expedicao liberada"));
    }
}
