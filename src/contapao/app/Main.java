
package contapao.app;

import contapao.catalogo.Insumo;
import contapao.catalogo.KitReposicao;
import contapao.expedicao.CentralExpedicaoMatriz;
import contapao.expedicao.GerenteFilial;
import contapao.expedicao.PainelExpedicaoCentral;
import contapao.fachada.PerfilAcesso;
import contapao.fachada.ReposicaoPadariaFacade;
import contapao.frete.FrotaPropriaFactory;
import contapao.integracao.AlertaSireneReal;
import contapao.persistencia.RepositorioReposicaoSQLite;
import contapao.rateio.RateioPorPrioridade;
import contapao.rateio.RateioProporcional;

import java.util.LinkedHashMap;
import java.util.Map;

public class Main {

    private static final String FARINHA = "Farinha de Trigo 50kg";

    public static void main(String[] args) {
        RepositorioReposicaoSQLite repositorio = new RepositorioReposicaoSQLite("padaria_demo.db");
        repositorio.inicializarBanco();
        repositorio.resetarDadosDemonstracao();

        PainelExpedicaoCentral painel = new PainelExpedicaoCentral();
        GerenteFilial gerenteA = new GerenteFilial("Padaria A");
        CentralExpedicaoMatriz.getInstance().registrar(painel);
        CentralExpedicaoMatriz.getInstance().registrar(gerenteA);

        ReposicaoPadariaFacade fachada = new ReposicaoPadariaFacade(repositorio, new RateioProporcional());
        fachada.setAlertaSonoro(new AlertaSireneReal());

        KitReposicao kit = new KitReposicao("Kit Fornada");
        kit.adicionar(new Insumo("Farinha", 4.50));
        kit.adicionar(new Insumo("Fermento", 1.20));

        Map<String, Integer> demandas = new LinkedHashMap<>();
        demandas.put("Padaria A", 70);
        demandas.put("Padaria B", 40);
        demandas.put("Padaria C", 40);

        System.out.println("Estoque inicial: " + repositorio.consultarSaldoCentral(FARINHA));
        try {
            fachada.confirmarDistribuicao(PerfilAcesso.GERENTE, 1, FARINHA, demandas);
        } catch (SecurityException e) {
            System.out.println("RN01 -> " + e.getMessage());
        }

        Map<String, Integer> resultado = fachada.confirmarDistribuicaoEExpedir(
                PerfilAcesso.CONTADOR, 1, FARINHA, kit, new FrotaPropriaFactory(), demandas);
        System.out.println("Distribuição (proporcional): " + resultado);
        System.out.println("Estoque final: " + repositorio.consultarSaldoCentral(FARINHA));

        fachada.getGuiaService().getGuiasEmitidas().forEach(g -> System.out.println("\n" + g.formatar()));

        fachada.trocarPolitica(new RateioPorPrioridade());
        try {
            fachada.confirmarDistribuicao(PerfilAcesso.CONTADOR, 1, FARINHA, demandas);
        } catch (IllegalStateException e) {
            System.out.println("\nRN05 -> " + e.getMessage());
        }

        System.out.println("\nPainel central: " + painel.getHistorico());
        System.out.println("Gerente da Padaria A: " + gerenteA.getMensagens());
    }
}