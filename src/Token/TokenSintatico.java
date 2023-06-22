package Token;

import java.util.ArrayList;
import java.util.List;

import Token.Enums.TipoToken;
import Token.Enums.TokenSintaticosEnum;

public class TokenSintatico extends Token {
    private List<Token> tokens;

    public TokenSintatico(TipoToken tipo, String lexema, List<Token> tokens) {
        super(tipo, lexema);
        this.tokens = tokens;
    }

    public TokenSintatico(TipoToken tipo, List<Token> tokens) {
        super(tipo, "");
        this.tokens = tokens;
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
    }

    public void imprimeToken(String tabulacao) {
        if (((this.getTipoToken().equals(TokenSintaticosEnum.COMANDO_COMPOSTO)) && (this.getTokens().size() == 1))) {
            for (Token token : tokens) {
                // if (token instanceof TokenSintatico || token instanceof TokenSemantico)
                token.imprimeToken(tabulacao);
            }
        } else {
            System.out.print(tabulacao + "<" + this.getTipoToken().getDescricao() + ">\n");
            for (Token token : tokens) {
                // if (token instanceof TokenSintatico || token instanceof TokenSemantico)
                token.imprimeToken(tabulacao + "\t");
            }
            System.out.print(tabulacao + "</" + this.getTipoToken().getDescricao() + ">\n");
        }
    }

    public List<String> imprimeErro() {
        List<String> erros = new ArrayList<>();
        for (Token token : tokens) {
            erros.addAll(token.imprimeErro());
        }
        return erros;
    }
}
