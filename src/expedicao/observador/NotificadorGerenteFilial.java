package expedicao.observador;

/** Observer que avisa o gerente de uma padaria específica (A, B ou C). */
public class NotificadorGerenteFilial implements ObservadorExpedicao {

    private final String nomeGerente;
    private final String codigoPadaria;

    public NotificadorGerenteFilial(String nomeGerente, String codigoPadaria) {
        if (nomeGerente == null || nomeGerente.isBlank()) {
            throw new IllegalArgumentException("O nome do gerente é obrigatório.");
        }
        if (codigoPadaria == null || !codigoPadaria.matches("[ABC]")) {
            throw new IllegalArgumentException("A padaria deve ser A, B ou C.");
        }
        this.nomeGerente = nomeGerente;
        this.codigoPadaria = codigoPadaria;
    }

    public String getNomeGerente() {
        return nomeGerente;
    }

    public String getCodigoPadaria() {
        return codigoPadaria;
    }

    @Override
    public void atualizar(String mensagem) {
        System.out.println("[Gerente " + nomeGerente + " - Padaria " + codigoPadaria + "] " + mensagem);
    }
}
