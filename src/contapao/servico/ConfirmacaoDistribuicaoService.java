package contapao.servico;

import contapao.expedicao.StatusCiclo;
import contapao.persistencia.RepositorioReposicao;
import contapao.rateio.PoliticaRateio;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConfirmacaoDistribuicaoService {

    private static final List<String> ORDEM_PRIORIDADE = List.of("Padaria A", "Padaria B", "Padaria C");

    private final RepositorioReposicao repositorio;
    private PoliticaRateio politicaRateio;

    public ConfirmacaoDistribuicaoService(RepositorioReposicao repositorio, PoliticaRateio politicaRateio) {
        this.repositorio = Objects.requireNonNull(repositorio, "Repositório não pode ser nulo.");
        this.politicaRateio = Objects.requireNonNull(politicaRateio, "Política de rateio não pode ser nula.");
    }

    public void trocarPolitica(PoliticaRateio novaPolitica) {
        this.politicaRateio = Objects.requireNonNull(novaPolitica, "Política de rateio não pode ser nula.");
    }

    public PoliticaRateio getPoliticaRateio() {
        return politicaRateio;
    }

    public Map<String, Integer> confirmar(int cicloId, String insumo, Map<String, Integer> demandasPorUnidade) {
        if (insumo == null || insumo.isBlank()) {
            throw new IllegalArgumentException("Insumo não informado.");
        }
        validarDemandas(demandasPorUnidade);

        StatusCiclo estado = repositorio.consultarEstadoCiclo(cicloId);
        if (estado == StatusCiclo.DISTRIBUIDO) {
            throw new IllegalStateException("Operação bloqueada: este ciclo já foi distribuído.");
        }
        if (!estado.podeDistribuir()) {
            throw new IllegalStateException(
                    "Transição inválida: o ciclo precisa estar ENCERRADO antes de distribuir (estado atual: " + estado + ").");
        }

        int estoqueDisponivel = repositorio.consultarSaldoCentral(insumo);

        Map<String, Integer> demandas = ordenarPorPrioridade(demandasPorUnidade);
        Map<String, Integer> distribuicao = politicaRateio.calcularDistribuicao(estoqueDisponivel, demandas);
        if (distribuicao == null) {
            throw new IllegalStateException("Violação de invariante: a política de rateio não devolveu resultado.");
        }

        int totalDistribuido = validarInvariantes(estoqueDisponivel, demandas, distribuicao);

        repositorio.registrarDistribuicaoAtomica(cicloId, insumo, totalDistribuido, distribuicao);
        return distribuicao;
    }

    private Map<String, Integer> ordenarPorPrioridade(Map<String, Integer> origem) {
        Map<String, Integer> ordenado = new LinkedHashMap<>();
        origem.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, Integer> e) -> {
                    int idx = ORDEM_PRIORIDADE.indexOf(e.getKey());
                    return idx >= 0 ? idx : Integer.MAX_VALUE;
                }).thenComparing(Map.Entry::getKey))
                .forEach(e -> ordenado.put(e.getKey(), e.getValue()));
        return ordenado;
    }

    private void validarDemandas(Map<String, Integer> demandas) {
        if (demandas == null || demandas.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma solicitação informada.");
        }
        for (Map.Entry<String, Integer> e : demandas.entrySet()) {
            if (e.getValue() == null || e.getValue() < 0) {
                throw new IllegalArgumentException("Quantidade solicitada inválida para a unidade " + e.getKey());
            }
        }
    }

    private int validarInvariantes(int estoque, Map<String, Integer> demandas, Map<String, Integer> distribuicao) {
        int total = 0;
        for (Map.Entry<String, Integer> e : distribuicao.entrySet()) {
            Integer pedido = demandas.get(e.getKey());
            Integer atendido = e.getValue();
            if (pedido == null || atendido == null || atendido < 0 || atendido > pedido) {
                throw new IllegalStateException("Violação de invariante: atendimento inválido para " + e.getKey());
            }
            total += atendido;
        }
        if (total > estoque) {
            throw new IllegalStateException("Violação de invariante: total distribuído excede o estoque disponível.");
        }
        return total;
    }
}