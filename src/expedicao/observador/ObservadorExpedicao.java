package expedicao.observador;

/** Contrato do Observer: recebe avisos sobre mudanças no ciclo de expedição. */
public interface ObservadorExpedicao {
    void atualizar(String mensagem);
}
