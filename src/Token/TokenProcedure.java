package Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static Token.Enums.TokensReservados.*;

public class TokenProcedure extends TokenIdentificador {

    private List<TokenIdentificador> parametros = new ArrayList<>();
    private Map<String, TokenIdentificador> listaIdentificadores = new HashMap<>();

    public TokenProcedure(Token token, List<TokenIdentificador> parametros) {
        super(token);
        if (parametros == null) {
            parametros = new ArrayList<>();
        }
        this.parametros = parametros;
    }

    public Boolean validaChamadaProcedure(List<Token> parametrosRecebidos) {
        if (this.getTipoToken().equals(WRITE) || this.getTipoToken().equals(WRITELN) ||
                this.getTipoToken().equals(READ) || this.getTipoToken().equals(READLN))
            return true;
        for (Integer each = 0; each < parametros.size(); each++) {
            if (parametrosRecebidos.size() <= each)
                return false;
            if (parametrosRecebidos.get(each) instanceof TokenSemantico) {
                if (!((TokenSemantico) parametrosRecebidos.get(each)).comparaTipoDado(parametros.get(each))) {
                    return false;
                }
            } else {
                if (parametrosRecebidos.get(each) instanceof TokenIdentificador) {
                    if (!((TokenIdentificador) parametrosRecebidos.get(each)).comparaTipoDado(parametros.get(each))) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }

        return parametros.size() == parametrosRecebidos.size();
    }

    public List<TokenIdentificador> getParametros() {
        return parametros;
    }

    public void setParametros(List<TokenIdentificador> parametros) {
        this.parametros = parametros;
    }

    public Map<String, TokenIdentificador> getListaIdentificadores() {
        return listaIdentificadores;
    }

    public void setListaIdentificadores(Map<String, TokenIdentificador> listaIdentificadores) {
        this.listaIdentificadores = listaIdentificadores;
    }

    public Token adicionaPalavraListaIdentificadores(String chave, Token token) {
        if (listaIdentificadores.get(chave) == null)
            listaIdentificadores.put(chave, new TokenIdentificador(token));
        return token;
    }

}
