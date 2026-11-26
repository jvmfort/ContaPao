package contapao.servico;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class GuiaRemessaService {

    private final AtomicInteger sequencia = new AtomicInteger();
    private final List<GuiaRemessa> emitidas = new CopyOnWriteArrayList<>();

    public GuiaRemessa gerarGuia(String codigoFilial, String resumoItens, double custoTotal) {
        if (codigoFilial == null || codigoFilial.isBlank()) {
            throw new IllegalArgumentException("Filial de destino não informada.");
        }
        if (resumoItens == null || resumoItens.isBlank()) {
            throw new IllegalArgumentException("Resumo de itens não informado.");
        }
        if (custoTotal < 0) {
            throw new IllegalArgumentException("Custo total não pode ser negativo.");
        }
        GuiaRemessa guia = new GuiaRemessa(String.format("REM-%04d", sequencia.incrementAndGet()),
                codigoFilial, resumoItens, custoTotal);
        emitidas.add(guia);
        return guia;
    }

    public List<GuiaRemessa> getGuiasEmitidas() {
        return Collections.unmodifiableList(emitidas);
    }
}
