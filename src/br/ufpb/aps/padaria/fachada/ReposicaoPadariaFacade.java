package br.ufpb.aps.padaria.fachada;

import catalogo.ItemReposicao;
import frete.CalculoFrete;
import frete.FreteFactory;
import frete.PedidoReposicao;
import br.ufpb.aps.padaria.integracao.AlertaSonoro;
import br.ufpb.aps.padaria.integracao.AlertaSonoroNulo;

/**
 * Fachada (Facade) que unifica os subsistemas de catálogo, frete,
 * expedição/alertas e serviços internos da matriz (estoque, financeiro e remessa)
 * em uma interface simples de alto nível.
 */
public class ReposicaoPadariaFacade {

    private final EstoqueMatrizService estoqueService;
    private final FinanceiroInternoService financeiroService;
    private final GuiaRemessaService guiaService;
    private AlertaSonoro alertaSonoro;

    public ReposicaoPadariaFacade(EstoqueMatrizService estoqueService,
                                 FinanceiroInternoService financeiroService,
                                 GuiaRemessaService guiaService,
                                 AlertaSonoro alertaSonoro) {
        this.estoqueService = estoqueService;
        this.financeiroService = financeiroService;
        this.guiaService = guiaService;
        this.alertaSonoro = (alertaSonoro != null) ? alertaSonoro : new AlertaSonoroNulo();
    }

    public ReposicaoPadariaFacade() {
        this(new EstoqueMatrizService(), new FinanceiroInternoService(), new GuiaRemessaService(), new AlertaSonoroNulo());
    }

    public void setAlertaSonoro(AlertaSonoro alertaSonoro) {
        this.alertaSonoro = (alertaSonoro != null) ? alertaSonoro : new AlertaSonoroNulo();
    }

    public EstoqueMatrizService getEstoqueService() {
        return estoqueService;
    }

    public FinanceiroInternoService getFinanceiroService() {
        return financeiroService;
    }

    public GuiaRemessaService getGuiaService() {
        return guiaService;
    }

    public AlertaSonoro getAlertaSonoro() {
        return alertaSonoro;
    }

    /**
     * Fluxo completo simplificado de reposição de itens para filiais:
     * 1. Valida disponibilidade de estoque (RN03)
     * 2. Calcula custo dos insumos (Composite) e frete (Factory Method / Strategy)
     * 3. Debita do financeiro interno
     * 4. Baixa o estoque central
     * 5. Emite alerta sonoro de expedição (Sirene real ou Null Object)
     * 6. Emite a guia de remessa interna
     */
    public boolean processarReposicao(String codigoFilial,
                                      String nomeInsumo,
                                      int qtdSolicitada,
                                      int qtdAtendida,
                                      ItemReposicao item,
                                      FreteFactory freteFactory) {

        System.out.println("\n--- Iniciando Reposicao para Filial: " + codigoFilial + " ---");

        // 1. Validação de estoque central (RN03)
        if (!estoqueService.validarDisponibilidade(nomeInsumo, qtdSolicitada, qtdAtendida)) {
            System.out.println("[Fachada] Reposicao cancelada: falha na validacao de estoque (RN03).");
            return false;
        }

        // 2. Cálculo dos custos de insumos e frete
        double valorInsumos = item.calcularCusto() * qtdAtendida;
        CalculoFrete calculoFrete = freteFactory.criarFrete();
        PedidoReposicao pedido = new PedidoReposicao(valorInsumos, calculoFrete);
        double custoTotal = pedido.calcularTotal();

        // 3. Débito no financeiro interno
        boolean debitoOk = financeiroService.debitarContaFilial(codigoFilial, custoTotal);
        if (!debitoOk) {
            System.out.println("[Fachada] Reposicao cancelada: falha no debito financeiro.");
            return false;
        }

        // 4. Baixa do estoque
        estoqueService.baixarEstoque(nomeInsumo, qtdAtendida);

        // 5. Emissão do alerta sonoro de expedição (sem verificar nulo graças ao Null Object Pattern)
        alertaSonoro.emitirAlerta("Expedicao liberada para filial " + codigoFilial + " | " + qtdAtendida + "x " + nomeInsumo);

        // 6. Emissão da guia de remessa
        String resumo = qtdAtendida + "x " + nomeInsumo;
        guiaService.gerarGuia(codigoFilial, resumo, custoTotal);

        System.out.println("--- Reposicao concluida com sucesso! ---\n");
        return true;
    }
}
