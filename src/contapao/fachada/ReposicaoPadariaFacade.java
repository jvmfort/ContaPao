package contapao.fachada;

import contapao.acesso.PerfilAcesso;
import contapao.catalogo.ItemReposicao;
import contapao.expedicao.CentralExpedicaoMatriz;
import contapao.frete.FreteFactory;
import contapao.frete.PedidoReposicao;
import contapao.integracao.AlertaSonoro;
import contapao.integracao.AlertaSonoroNulo;
import contapao.persistencia.RepositorioReposicao;
import contapao.rateio.PoliticaRateio;
import contapao.servico.ConfirmacaoDistribuicaoService;
import contapao.servico.EstoqueMatrizService;
import contapao.servico.FinanceiroInternoService;
import contapao.servico.GuiaRemessaService;

import java.util.Map;
import java.util.Objects;

public class ReposicaoPadariaFacade {

    private final RepositorioReposicao repositorio;
    private final ConfirmacaoDistribuicaoService distribuicaoService;
    private final EstoqueMatrizService estoqueService;
    private final FinanceiroInternoService financeiroService;
    private final GuiaRemessaService guiaService;
    private final CentralExpedicaoMatriz central = CentralExpedicaoMatriz.getInstance();
    private AlertaSonoro alertaSonoro;

    public ReposicaoPadariaFacade(RepositorioReposicao repositorio,
                                  PoliticaRateio politicaRateio,
                                  FinanceiroInternoService financeiroService,
                                  GuiaRemessaService guiaService,
                                  AlertaSonoro alertaSonoro) {
        this.repositorio = Objects.requireNonNull(repositorio, "Repositório não pode ser nulo.");
        this.distribuicaoService = new ConfirmacaoDistribuicaoService(repositorio, politicaRateio);
        this.estoqueService = new EstoqueMatrizService(repositorio);
        this.financeiroService = Objects.requireNonNull(financeiroService, "Financeiro não pode ser nulo.");
        this.guiaService = Objects.requireNonNull(guiaService, "Serviço de guia não pode ser nulo.");
        this.alertaSonoro = (alertaSonoro != null) ? alertaSonoro : new AlertaSonoroNulo();
    }

    public ReposicaoPadariaFacade(RepositorioReposicao repositorio, PoliticaRateio politicaRateio) {
        this(repositorio, politicaRateio, new FinanceiroInternoService(), new GuiaRemessaService(), null);
    }

    public void trocarPolitica(PoliticaRateio novaPolitica) {
        distribuicaoService.trocarPolitica(novaPolitica);
    }

    public void setAlertaSonoro(AlertaSonoro alertaSonoro) {
        this.alertaSonoro = (alertaSonoro != null) ? alertaSonoro : new AlertaSonoroNulo();
    }

    public AlertaSonoro getAlertaSonoro() {
        return alertaSonoro;
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

    public void abrirCiclo(PerfilAcesso perfil, int cicloId) {
        exigirPerfil(perfil).exigirGerenciarCiclo();
        repositorio.criarCiclo(cicloId);
    }

    public void encerrarCiclo(PerfilAcesso perfil, int cicloId) {
        exigirPerfil(perfil).exigirGerenciarCiclo();
        repositorio.encerrarCiclo(cicloId);
        central.notificarEncerramento(cicloId);
    }

    public Map<String, Integer> confirmarDistribuicao(PerfilAcesso perfil, int cicloId, String insumo,
                                                      Map<String, Integer> demandasPorUnidade) {
        exigirPerfil(perfil).exigirConfirmarDistribuicao();
        Map<String, Integer> distribuicao = distribuicaoService.confirmar(cicloId, insumo, demandasPorUnidade);
        central.notificarDistribuicao(cicloId, insumo, distribuicao);
        return distribuicao;
    }

    public Map<String, Integer> confirmarDistribuicaoEExpedir(PerfilAcesso perfil, int cicloId, String insumo,
                                                              ItemReposicao item, FreteFactory freteFactory,
                                                              Map<String, Integer> demandasPorUnidade) {
        Objects.requireNonNull(item, "Item de reposição não pode ser nulo.");
        Objects.requireNonNull(freteFactory, "Fábrica de frete não pode ser nula.");

        Map<String, Integer> distribuicao = confirmarDistribuicao(perfil, cicloId, insumo, demandasPorUnidade);

        for (Map.Entry<String, Integer> e : distribuicao.entrySet()) {
            int qtd = e.getValue();
            if (qtd <= 0) {
                continue;
            }
            double custoTotal = calcularCustoTotal(item, qtd, freteFactory);
            if (!financeiroService.debitarContaFilial(e.getKey(), custoTotal)) {
                throw new IllegalStateException("Distribuição confirmada, mas o débito da filial " + e.getKey() + " falhou.");
            }
            emitirExpedicao(e.getKey(), insumo, qtd, custoTotal);
        }
        return distribuicao;
    }

    public boolean processarReposicao(String codigoFilial, String nomeInsumo, int qtdSolicitada, int qtdAtendida,
                                      ItemReposicao item, FreteFactory freteFactory) {
        Objects.requireNonNull(item, "Item de reposição não pode ser nulo.");
        Objects.requireNonNull(freteFactory, "Fábrica de frete não pode ser nula.");

        if (qtdSolicitada <= 0 || qtdAtendida <= 0) {
            return false;
        }
        if (!estoqueService.validarDisponibilidade(nomeInsumo, qtdSolicitada, qtdAtendida)) {
            return false;
        }

        double custoTotal = calcularCustoTotal(item, qtdAtendida, freteFactory);

        if (!estoqueService.baixarEstoque(nomeInsumo, qtdAtendida)) {
            return false;
        }
        if (!financeiroService.debitarContaFilial(codigoFilial, custoTotal)) {
            estoqueService.cadastrarEstoque(nomeInsumo, qtdAtendida);
            return false;
        }

        emitirExpedicao(codigoFilial, nomeInsumo, qtdAtendida, custoTotal);
        return true;
    }

    private PerfilAcesso exigirPerfil(PerfilAcesso perfil) {
        if (perfil == null) {
            throw new SecurityException("Perfil de acesso não informado.");
        }
        return perfil;
    }

    private double calcularCustoTotal(ItemReposicao item, int quantidade, FreteFactory freteFactory) {
        PedidoReposicao pedido = new PedidoReposicao(item.calcularCusto() * quantidade, freteFactory.criarFrete());
        return pedido.calcularTotal();
    }

    private void emitirExpedicao(String codigoFilial, String insumo, int quantidade, double custoTotal) {
        alertaSonoro.emitirAlerta("Expedicao liberada para filial " + codigoFilial + " | " + quantidade + "x " + insumo);
        guiaService.gerarGuia(codigoFilial, quantidade + "x " + insumo, custoTotal);
    }
}