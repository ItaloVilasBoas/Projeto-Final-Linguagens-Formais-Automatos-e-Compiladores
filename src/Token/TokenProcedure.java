package Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static Token.Enums.TokensReservados.*;

public class TokenProcedure extends TokenIdentificador {

    private List<TokenIdentificador> parametros = new ArrayList<>();
    private Token comandos;
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
                if (!comparaTipoDado(parametros.get(each).getTipoDado(),
                        ((TokenSemantico) parametrosRecebidos.get(each)).getTipoDado()))
                    return false;

            } else {
                if (parametrosRecebidos.get(each) instanceof TokenIdentificador) {
                    if (!comparaTipoDado(parametros.get(each).getTipoDado(),
                            ((TokenIdentificador) parametrosRecebidos.get(each)).getTipoDado()))
                        return false;
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

    private static Boolean comparaTipoDado(String tipo1, String tipo2) {
        if (tipo1.equals(REAL.getDescricao())) {
            return (tipo2.equals(REAL.getDescricao()) || tipo2.equals(INTEGER.getDescricao()));
        }
        return tipo1.equals(tipo2);
    }

    public Token getComandos() {
        return comandos;
    }

    public void setComandos(Token comandos) {
        this.comandos = comandos;
    }

}
