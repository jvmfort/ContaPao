
package contapao.expedicao;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CentralExpedicaoMatriz {

    private static final class Holder {
        private static final CentralExpedicaoMatriz INSTANCIA = new CentralExpedicaoMatriz();
    }

    private final List<ObservadorExpedicao> observadores = new CopyOnWriteArrayList<>();

    private CentralExpedicaoMatriz() {
    }

    public static CentralExpedicaoMatriz getInstance() {
        return Holder.INSTANCIA;
    }

    public void registrar(ObservadorExpedicao observador) {
        Objects.requireNonNull(observador, "Observador não pode ser nulo.");
        if (!observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    public void remover(ObservadorExpedicao observador) {
        observadores.remove(observador);
    }

    public void limparObservadores() {
        observadores.clear();
    }

    public int totalObservadores() {
        return observadores.size();
    }

    public void notificarEncerramento(int cicloId) {
        for (ObservadorExpedicao o : observadores) {
            try {
                o.aoEncerrarCiclo(cicloId);
            } catch (RuntimeException ignored) {
            }
        }
    }

    public void notificarDistribuicao(int cicloId, String insumo, Map<String, Integer> atendimentos) {
        for (ObservadorExpedicao o : observadores) {
            try {
                o.aoDistribuirCiclo(cicloId, insumo, atendimentos);
            } catch (RuntimeException ignored) {
            }
        }
    }
}