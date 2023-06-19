package Token;

import Token.Enums.TipoToken;
import Token.Enums.TokenGenerico;
import Token.Enums.TokenSintaticosEnum;

public class Token {

    private TipoToken tipoToken;
    private String lexema;

    public Token(TipoToken tipo, String lexema) {
        this.tipoToken = tipo;
        this.lexema = lexema;
    }

    public TipoToken getTipoToken() {
        return tipoToken;
    }

    public void setTipoToken(TipoToken tipo) {
        this.tipoToken = tipo;
    }

    public String getLexema() {
        return lexema;
    }

    public void setLexema(String lexema) {
        this.lexema = lexema;
    }

    @Override
    public String toString() {
        if (this.getTipoToken().equals(TokenSintaticosEnum.ERRO_SINTATICO)
                || this.getTipoToken().equals(TokenGenerico.ERRO))
            return String.format("\u001B[31m<%s, %s>\u001B[0m", this.getTipoToken().getDescricao(), this.getLexema());
        return String.format("\u001B[32m<%s, %s>\u001B[0m", this.getTipoToken().getDescricao(), this.getLexema());
    }

    public void imprimeToken(String tabulacao) {
        System.out.println(tabulacao + this);
    }

    public void imprimeErro() {
        if (this.getTipoToken().equals(TokenGenerico.ERRO))
            System.out.println(this.getLexema());
    }

}
