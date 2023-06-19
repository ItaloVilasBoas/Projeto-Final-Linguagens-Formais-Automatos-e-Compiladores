package Exceptions;

public class AnaliseException extends Exception {
    private String tipo;

    public AnaliseException(String mensagem, String tipo) {
        super(mensagem);
        this.tipo = tipo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

}
