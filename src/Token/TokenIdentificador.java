package Token;

public class TokenIdentificador extends Token {

    private String valor;

    private String categoria;

    private String tipoDado;

    public TokenIdentificador(Token token) {
        super(token.getTipo(), token.getLexema());
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getTipoDado() {
        return tipoDado;
    }

    public void setTipoDado(String tipoDado) {
        this.tipoDado = tipoDado;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Boolean comparaTipoDado(TokenIdentificador tkComparar) {
        return tkComparar.getTipoDado().equals(this.getTipoDado());
    }
}
