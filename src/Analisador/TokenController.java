package Analisador;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Token.Token;
import Token.TokenIdentificador;
import Token.TokenProcedure;
import Token.Enums.EstadosEnum;
import static Token.Enums.TokenGenerico.*;
import Token.Enums.TokensReservados;

public class TokenController {

    private static Map<String, Token> listaPalavrasReservadas = new HashMap<>();
    private static Map<String, TokenIdentificador> listaIdentificadores = new HashMap<>();
    private static Map<String, TokenProcedure> listaProcedures = new HashMap<>();

    public TokenController() {
        Arrays.asList(TokensReservados.values()).stream().forEach(tipo -> {
            adicionaPalavraListaReservadas(tipo.getDescricao(), new Token(tipo, tipo.toString()));
        });
        // INICIAR LISTA PROCEDURES COM AS PROCEDURES DO PROGRAMA
    }

    public static Token getToken(EstadosEnum estadoEnum, String palavra) {
        Token testePalavraReservada = listaPalavrasReservadas.get(palavra.toUpperCase());
        if (palavra.trim().equals(""))
            return null;
        if (testePalavraReservada != null)
            return testePalavraReservada;
        switch (estadoEnum) {
            case LIDO_IDENTIFICADOR:
                return adicionaPalavraListaIdentificadores(palavra, new Token(ID, palavra));
            case LIDO_NUMERO_INTEIRO:
                return new Token(VALORINT, palavra);
            case LIDO_NUMERO_REAL:
                return new Token(VALOREAL, palavra);
            case LIDO_LITERAL:
                return new Token(VALORLIT, palavra.replaceAll("#", ""));
            case LIDO_CARACTER_ESPECIAL:
            case LIDO_NUMERO_REAL_INCOMPLETO:
            case LIDO_CARACTER_INVALIDO:
            case LIDO_LITERAL_ERRO:
                return new Token(ERRO, "Lexical error, Uncrecognized SYMBOL " + palavra + " not expected");
            case LIDO_LITERAL_INCOMPLETO:
                return new Token(ERRO, "Lexical error, String literal is not properly closed");
            default:
                return new Token(ERRO, "Token nao identificado");
        }
    }

    public static Map<String, TokenIdentificador> getListaIdentificadores() {
        return listaIdentificadores;
    }

    public static Token adicionaPalavraListaIdentificadores(String chave, Token token) {
        listaIdentificadores.put(chave, new TokenIdentificador(token));
        return token;
    }

    public static Map<String, TokenProcedure> getListaProcedures() {
        return listaProcedures;
    }

    public static Token adicionaPalavraListaProcedures(String chave, Token token, List<TokenIdentificador> parametros) {
        listaProcedures.put(chave, new TokenProcedure(token, parametros));
        return token;
    }

    public static TokenIdentificador getTkIds(String palavra) {
        return listaIdentificadores.get(palavra);
    }

    public static void updateTipoDadoTkIds(String palavra, String tipo) {
        listaIdentificadores.get(palavra).setTipoDado(tipo);
    }

    public static void updateCategoriaVarTkIds(String palavra, String categoria) {
        listaIdentificadores.get(palavra).setCategoria(categoria);
    }

    public Token adicionaPalavraListaReservadas(String chave, Token token) {
        listaPalavrasReservadas.put(chave, token);
        return token;
    }

    public Token getTokenPalavraReservada(String palavra) {
        return listaPalavrasReservadas.get(palavra.toUpperCase());
    }
}
