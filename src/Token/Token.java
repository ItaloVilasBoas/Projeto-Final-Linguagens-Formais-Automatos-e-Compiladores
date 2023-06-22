package Token;

import java.util.ArrayList;
import java.util.List;

import Token.Enums.TipoToken;
import Token.Enums.TokenGenerico;
import Token.Enums.TokenSintaticosEnum;

public class Token {

    private TipoToken tipoToken;
    private String lexema;
    private String linha;

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

    public String getLinha() {
        return linha;
    }

    public void setLinha(String linha) {
        this.linha = linha;
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

    public List<String> imprimeErro() {
        List<String> erros = new ArrayList<>();
        if (this.getTipoToken().equals(TokenGenerico.ERRO))
            erros.add(this.getLexema() + ", line " + linha);
        return erros;
    }

}
