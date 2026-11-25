package br.ufpb.aps.padaria.integracao;

public class AlertaSonoroNulo implements AlertaSonoro {

    @Override
    public void emitirAlerta(String msg) {
        // Modo silencioso ativo: operação vazia (no-op) para evitar NullPointerException
    }
}
