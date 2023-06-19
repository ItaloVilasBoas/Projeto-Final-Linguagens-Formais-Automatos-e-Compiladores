package Token;

import java.util.List;

import Token.Enums.TipoToken;

public class TokenSemantico extends TokenSintatico {

    private String valor;

    private String tipoDado;

    public TokenSemantico(TipoToken tipo, List<Token> tokens) {
        super(tipo, tokens);
    }

    public TokenSemantico(TokenSintatico tokenSintatico, String valor, String tipoDado) {
        super(tokenSintatico.getTipoToken(), tokenSintatico.getTokens());
        this.valor = valor;
        this.tipoDado = tipoDado;
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

    public void imprimeToken(String tabulacao) {

        System.out.print(
                tabulacao + "\u001B[34m<" + this.getTipoToken().getDescricao() + "," + this.getTipoDado()
                        + ">\u001B[0m\n");
        for (Token token : this.getTokens()) {
            // if (token instanceof TokenSintatico || token instanceof TokenSemantico)
            token.imprimeToken(tabulacao + "\t");
        }
        System.out.print(tabulacao + "\u001B[34m</" + this.getTipoToken().getDescricao() + ">\u001B[0m\n");
    }

    public Boolean comparaTipoDado(TokenIdentificador tkComparar) {
        return tkComparar.getTipoDado().equals(this.getTipoDado());
    }

    public Boolean comparaTipoDado(TokenSemantico tkComparar) {
        return tkComparar.getTipoDado().equals(this.getTipoDado());
    }
}
