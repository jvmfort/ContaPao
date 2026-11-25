package expedicao.observador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Observer do Contador / Gestor Central: acompanha e registra a consolidação. */
public class NotificadorPainelCentral implements ObservadorExpedicao {

    private final List<String> historico = new ArrayList<>();

    @Override
    public void atualizar(String mensagem) {
        historico.add(mensagem);
        System.out.println("[Painel Central] " + mensagem);
    }

    public List<String> getHistorico() {
        return Collections.unmodifiableList(historico);
    }
}
