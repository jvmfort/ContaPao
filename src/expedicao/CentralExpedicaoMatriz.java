package expedicao;

import expedicao.observador.ObservadorExpedicao;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton + Subject do Observer. Guarda o estado do ciclo (RN01) e
 * impede que a distribuição seja confirmada duas vezes (RN05).
 */
public class CentralExpedicaoMatriz {

    private static final CentralExpedicaoMatriz INSTANCIA = new CentralExpedicaoMatriz();

    private final List<ObservadorExpedicao> observadores = new ArrayList<>();
    private StatusCiclo status = StatusCiclo.ABERTO;
    private boolean distribuicaoConfirmada = false;

    private CentralExpedicaoMatriz() {
    }

    public static CentralExpedicaoMatriz getInstancia() {
        return INSTANCIA;
    }

    public synchronized void adicionarObservador(ObservadorExpedicao o) {
        if (o != null && !observadores.contains(o)) {
            observadores.add(o);
        }
    }

    public synchronized void removerObservador(ObservadorExpedicao o) {
        observadores.remove(o);
    }

    public synchronized StatusCiclo getStatus() {
        return status;
    }

    public synchronized boolean isDistribuicaoConfirmada() {
        return distribuicaoConfirmada;
    }

    /** RN01: lança IllegalStateException se o ciclo não aceita mais solicitações. */
    public synchronized void validarAceitaSolicitacoes() {
        if (!status.aceitaSolicitacoes()) {
            throw new IllegalStateException("Ciclo " + status + ": não aceita envio nem edição de solicitações.");
        }
    }

    /** ABERTO -> ENCERRADO. */
    public synchronized void encerrarCiclo() {
        transitar(StatusCiclo.ENCERRADO);
        notificar("Ciclo encerrado: novas solicitações não são mais aceitas.");
    }

    /**
     * ENCERRADO -> DISTRIBUIDO. Idempotente (RN05): a repetição retorna false,
     * não altera o estado e não notifica ninguém.
     *
     * @return true se esta chamada confirmou; false se já estava confirmada
     */
    public synchronized boolean confirmarDistribuicao() {
        if (distribuicaoConfirmada) {
            return false;
        }
        transitar(StatusCiclo.DISTRIBUIDO);
        distribuicaoConfirmada = true;
        notificar("Distribuição confirmada: estoques das padarias atualizados.");
        return true;
    }

    /** Volta ao estado inicial (ABERTO, sem observadores): demonstração e testes. */
    public synchronized void reiniciar() {
        status = StatusCiclo.ABERTO;
        distribuicaoConfirmada = false;
        observadores.clear();
    }

    private void transitar(StatusCiclo destino) {
        if (!status.podeTransitarPara(destino)) {
            throw new IllegalStateException("Transição inválida: " + status + " -> " + destino + ".");
        }
        status = destino;
    }

    private void notificar(String mensagem) {
        for (ObservadorExpedicao o : new ArrayList<>(observadores)) {
            o.atualizar(mensagem);
        }
    }
}
