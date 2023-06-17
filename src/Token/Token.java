package Token;

import Token.Enums.TipoToken;
import Token.Enums.TokenGenerico;
import Token.Enums.TokenSintaticosEnum;

public class Token {

    private TipoToken tipo;
    private String lexema;

    public Token(TipoToken tipo, String lexema) {
        this.tipo = tipo;
        this.lexema = lexema;
    }

    public TipoToken getTipo() {
        return tipo;
    }

    public void setTipo(TipoToken tipo) {
        this.tipo = tipo;
    }

    public String getLexema() {
        return lexema;
    }

    public void setLexema(String lexema) {
        this.lexema = lexema;
    }

    @Override
    public String toString() {
        if (this.getTipo().equals(TokenSintaticosEnum.ERRO_SINTATICO) || this.getTipo().equals(TokenGenerico.ERRO))
            return String.format("\u001B[31m<%s, %s>\u001B[0m", this.getTipo().getDescricao(), this.getLexema());
        return String.format("\u001B[32m<%s, %s>\u001B[0m", this.getTipo().getDescricao(), this.getLexema());
    }

    public void imprimeToken(String tabulacao) {
        System.out.println(tabulacao + this);
    }

}
