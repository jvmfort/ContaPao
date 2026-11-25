package br.ufpb.aps.padaria.integracao;

public class AlertaSireneReal implements AlertaSonoro {

    @Override
    public void emitirAlerta(String msg) {
        System.out.println("[SIRENE EXPEDIÇÃO] ALERTA SONORO ATIVADO: " + msg);
    }
}
