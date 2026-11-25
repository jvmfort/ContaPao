package rateio;

import catalogo.SolicitacaoReposicao;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ResultadoRateio {

    private final List<SolicitacaoReposicao> itensAlocados;
    private final int totalDistribuido;
    private final int residuoDistribuido;

    public ResultadoRateio(List<SolicitacaoReposicao> itensAlocados, int totalDistribuido, int residuoDistribuido) {
        this.itensAlocados = Collections.unmodifiableList(Objects.requireNonNull(itensAlocados));
        this.totalDistribuido = totalDistribuido;
        this.residuoDistribuido = residuoDistribuido;
    }

    public List<SolicitacaoReposicao> getItensAlocados(){
        return itensAlocados;
    }

    public int getTotalDistribuido() {
        return totalDistribuido;
    }

    public int getResiduoDistribuido() {
        return residuoDistribuido;
    }
}
