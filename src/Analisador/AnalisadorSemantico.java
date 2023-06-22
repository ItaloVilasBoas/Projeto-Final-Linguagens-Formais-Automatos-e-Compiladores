package Analisador;

import Token.TokenSintatico;
import Token.Enums.TipoToken;
import Token.Enums.TokenGenerico;
import Token.Enums.TokenSintaticosEnum;
import static Token.Enums.TokenGenerico.*;
import static Token.Enums.TokensReservados.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import Exceptions.AnaliseException;
import Token.Token;
import Token.TokenIdentificador;
import Token.TokenProcedure;
import Token.TokenSemantico;

public class AnalisadorSemantico {

    public static TokenSemantico analisaSemantica(TokenSintatico sintatico, Map<String, TokenIdentificador> listaTkId)
            throws AnaliseException {

        for (Token t : sintatico.getTokens()) {
            if (t.getTipoToken().equals(ERRO)) {
                throw new AnaliseException(t.getLexema(), "Syntax");
            }
        }

        switch (getTipoEnumSintatico(sintatico)) {
            case ESTRUTURA_ATRIBUICAO:
                return analisaAtribuicao(sintatico, listaTkId);
            case LISTA_EXPRESSAO:
                return analisaListaExpressao(sintatico, listaTkId);
            case CONDICIONAL:
                return analisaCondicional(sintatico);
            case CHAMADA_PROCEDIMENTO:
                return analisaChamadaProcedimento(sintatico);
            case REPETITIVO:
                return analisaRepetitivo(sintatico);
            default:
                break;
        }
        throw new AnaliseException("Semantic Erro!", "Semantic");
    }

    private static TokenSemantico analisaRepetitivo(TokenSintatico sintatico) throws AnaliseException {
        var tipoRepetitivo = sintatico.getTokens().get(0);
        if (tipoRepetitivo.getTipoToken().equals(WHILE) || tipoRepetitivo.getTipoToken().equals(REPEAT)) {
            return analisaCondicional(sintatico);
        }
        TokenSemantico atribuicao = (TokenSemantico) sintatico.getTokens().get(1);
        Token toDownTo = sintatico.getTokens().get(2);
        TokenSemantico expressao = (TokenSemantico) sintatico.getTokens().get(3);

        String repetivo = "FOR " + atribuicao.getTokens().get(0).getLexema();
        repetivo += atribuicao.getTokens().get(1).getTipoToken().getDescricao();
        repetivo += ((TokenSemantico) atribuicao.getTokens().get(2)).getValor();
        repetivo += toDownTo.getLexema() + " " + expressao.getValor();
        repetivo += ", line " + expressao.getTokens().get(0).getLinha();

        String mA = "\nType mismatch: cannot convert from " + atribuicao.getTipoDado() + " to INTEGER";
        String mE = "\nType mismatch: cannot convert from " + expressao.getTipoDado() + " to INTEGER";

        if (!atribuicao.getTipoDado().equals(INTEGER.getDescricao()))
            throw new AnaliseException(repetivo + mA, "Semantic");
        if (!expressao.getTipoDado().equals(INTEGER.getDescricao()))
            throw new AnaliseException(repetivo + mE, "Semantic");

        return new TokenSemantico(sintatico, expressao.getValor(),
                toDownTo.getLexema() + " " + expressao.getValor());
    }

    private static TokenSemantico analisaChamadaProcedimento(TokenSintatico sintatico) throws AnaliseException {
        Token idProcedure = sintatico.getTokens().get(0);
        String lex = idProcedure.getLexema().toUpperCase();

        List<Token> listaParametros = new ArrayList<>();

        for (Token token : sintatico.getTokens().subList(1, sintatico.getTokens().size())) {
            if (token.getTipoToken().equals(TokenSintaticosEnum.LISTA_EXPRESSAO)) {
                listaParametros.add(token);
            }
        }

        if (TokenController.getListaProcedures().get(lex) == null)
            throw new AnaliseException("The procedure " + lex + " is undefined, line " + idProcedure.getLinha(),
                    "Semantic");

        if (!TokenController.getListaProcedures().get(lex).validaChamadaProcedure(listaParametros))
            throw new AnaliseException(
                    "The procedure " + lex + " is not applicable for the given arguments, line "
                            + idProcedure.getLinha(),
                    "Semantic");
        return new TokenSemantico(sintatico, "", "");
    }

    private static TokenSemantico analisaCondicional(TokenSintatico sintatico) throws AnaliseException {
        List<Token> lstTokens = sintatico.getTokens();

        TokenSemantico expressao = (TokenSemantico) lstTokens.get(1);

        if (expressao.getTipoDado().equals(BOOLEAN.getDescricao())) {
            return new TokenSemantico(sintatico, expressao.getValor(), expressao.getTipoDado());
        }

        String m = "Type mismatch: cannot convert from " + expressao.getTipoDado() + " to BOOLEAN in: IF ";
        m += expressao.getValor() + " THEN, line " + lstTokens.get(0).getLinha();
        throw new AnaliseException(m, "Semantic");
    }

    private static TokenSemantico analisaAtribuicao(TokenSintatico sintatico, Map<String, TokenIdentificador> listaTkId)
            throws AnaliseException {
        List<Token> lstTokens = sintatico.getTokens();
        TokenIdentificador identificador = listaTkId.get(lstTokens.get(0).getLexema().toUpperCase());
        TokenSemantico expressao = (TokenSemantico) lstTokens.get(2);

        if (identificador == null || identificador.getTipoDado() == null) {
            String m = sintatico.getTokens().get(0).getLexema().toUpperCase();
            m += " cannot be resolved to a variable, line ";
            m += sintatico.getTokens().get(0).getLinha();
            throw new AnaliseException(m, "Semantic");
        }
        if (isCompativelAtribuicao(identificador.getTipoDado(), expressao.getTipoDado())) {
            return new TokenSemantico(sintatico, expressao.getValor(), identificador.getTipoDado());
        }

        String m = "Type mismatch: cannot convert from " + expressao.getTipoDado();
        m += " to " + identificador.getTipoDado();
        m += " in line " + lstTokens.get(0).getLinha();
        throw new AnaliseException(m, "Semantic");
    }

    private static boolean comparaLstToken(Token tokenRecebido, List<TipoToken> tokensEsperados) {
        for (TipoToken tipoToken : tokensEsperados) {
            if (tokenRecebido.getTipoToken().equals(tipoToken)) {
                return true;
            }
        }
        return false;
    }

    private static TokenSemantico analisaListaExpressao(TokenSintatico sintatico,
            Map<String, TokenIdentificador> listaTkId) throws AnaliseException {

        List<Token> lstTokens = sintatico.getTokens();
        String expressaoValor = "", expressaoTipoDado = "";

        List<Token> analisaExpressao;
        if (comparaLstToken(lstTokens.get(lstTokens.size() - 1),
                List.of(TO, DOWNTO, DO, FDEFIN, THEN, ELSE, SEPLISTA, RPAREN))) {
            analisaExpressao = lstTokens.subList(0, lstTokens.size() - 1);
        } else {
            analisaExpressao = lstTokens.subList(0, lstTokens.size());
        }

        for (Token token : analisaExpressao) {
            if (token.getTipoToken().equals(ID)) {
                TokenIdentificador identificador = listaTkId.get(token.getLexema().toUpperCase());
                if (identificador == null || identificador.getTipoDado() == null) {
                    String m = token.getLexema().toUpperCase() + " cannot be resolved to a variable, line ";
                    m += token.getLinha();
                    throw new AnaliseException(m, "Semantic");
                }
                expressaoValor += identificador.getLexema() + " ";
                expressaoTipoDado += identificador.getTipoDado() + " ";
            } else if (token.getTipoToken().equals(TRUE) || token.getTipoToken().equals(FALSE)) {
                expressaoValor += token.getTipoToken().getDescricao() + " ";
                expressaoTipoDado += "BOOLEAN ";
            } else if (token.getTipoToken() instanceof TokenGenerico) {
                expressaoValor += token.getLexema() + " ";
                expressaoTipoDado += token.getTipoToken().getDescricao() + " ";
            } else {
                expressaoValor += token.getTipoToken().getDescricao() + " ";
                expressaoTipoDado += token.getTipoToken().getDescricao() + " ";
            }
        }

        ArrayList<String> expressoes = new ArrayList<>(Arrays.asList(expressaoTipoDado.split(" ")));
        expressaoTipoDado = analisaTipoDado(expressoes);

        return new TokenSemantico(sintatico, expressaoValor, expressaoTipoDado);
    }

    private static Integer procuraOperador(List<TipoToken> tipos, List<String> expressoes) {
        Integer menor = -1;
        for (TipoToken tipo : tipos) {
            var posOperador = expressoes.indexOf(tipo.getDescricao());
            if ((posOperador != -1 && menor == -1) || (posOperador < menor && posOperador != -1)) {
                menor = posOperador;
            }
        }
        return menor;
    }

    private static Integer posicaoRparen(List<String> expressoes) {
        Integer skip = 0, pos;

        for (pos = 0; pos < expressoes.size(); pos++) {
            String expressao = expressoes.get(pos);
            if (expressao.equals(LPAREN.getDescricao())) {
                skip += 1;
            } else if (expressao.equals(RPAREN.getDescricao())) {
                if (skip > 0) {
                    skip -= 1;
                } else {
                    return pos;
                }
            }
        }
        return pos;
    }

    private static String analisaTipoDado(List<String> expressoes) throws AnaliseException {
        while (expressoes.size() != 1) {
            int lparen = procuraOperador(List.of(LPAREN), expressoes);
            int not = procuraOperador(List.of(OPALOGNOT), expressoes);
            int and = procuraOperador(List.of(OPALOGAND), expressoes);
            int or = procuraOperador(List.of(OPALOGOR), expressoes);
            int p2 = procuraOperador(List.of(OPARITMULT, OPARITDIV, OPARITDIVINT, OPARITMOD), expressoes);
            int p3 = procuraOperador(List.of(OPARITSOMA, OPARITSUB), expressoes);
            int rel = procuraOperador(List.of(OPRELEQUAL, OPRELGREAT, OPRELLESS, OPRELGEQUAL, OPRELLEQUAL, OPRELNEQUAL),
                    expressoes);

            int andOr = and != -1 ? and : (or != -1 ? or : -1);
            int prec = lparen != -1 ? lparen : (p2 != -1 ? p2 : (p3 != -1 ? p3 : rel));

            if (lparen != -1) {
                int rparen = posicaoRparen(expressoes.subList(lparen + 1, expressoes.size()));
                var copiaTrecho = new ArrayList<>(expressoes.subList(lparen + 1, lparen + rparen + 1));
                expressoes.subList(lparen, lparen + rparen + 2).clear();
                var tipo = analisaTipoDado(copiaTrecho);
                expressoes.add(lparen, tipo);
            } else if (not != -1) {
                expressoes.remove(not);
            } else if (andOr != -1) {
                if (!expressoes.get(andOr + 1).equals(BOOLEAN.getDescricao())
                        || !expressoes.get(andOr - 1).equals(BOOLEAN.getDescricao())) {
                    String m = "The operator " + expressoes.get(andOr) + " is undefined for the argument type(s) ";
                    m += expressoes.get(andOr - 1) + " and " + expressoes.get(andOr + 1);
                    throw new AnaliseException(m, "Semantic");
                }
                expressoes.remove(andOr);
                expressoes.remove(andOr);
            } else if (prec != -1) {
                // if (!expressoes.get(prec - 1).equals(expressoes.get(prec + 1))) {
                if (!isCompativel(expressoes.get(prec - 1), expressoes.get(prec + 1), expressoes.get(prec))) {
                    String m = "The operator " + expressoes.get(prec) + " is undefined for the argument type(s) ";
                    m += expressoes.get(prec - 1) + " and " + expressoes.get(prec + 1);
                    throw new AnaliseException(m, "Semantic");
                } else if ((p2 == -1) && (p3 == -1)) {
                    expressoes.set(prec - 1, "BOOLEAN");
                }

                if (expressoes.get(prec).equals(OPARITDIV.getDescricao())) {
                    expressoes.set(prec - 1, "REAL");
                }
                expressoes.remove(prec);
                expressoes.remove(prec);
            }
        }

        return expressoes.get(0);
    }

    private static Boolean isCompativel(String tipo1, String tipo2, String operacao) {
        if (tipo1.equals(INTEGER.getDescricao())) {
            if (tipo2.equals(INTEGER.getDescricao()))
                return true;
            if (tipo2.equals(REAL.getDescricao())) {
                if (operacao.equals(OPARITSOMA.getDescricao()) || operacao.equals(OPARITSUB.getDescricao())
                        || operacao.equals(OPARITDIV.getDescricao()) || operacao.equals(OPARITMULT.getDescricao())
                        || operacao.equals(OPRELEQUAL.getDescricao()) || operacao.equals(OPRELGREAT.getDescricao())
                        || operacao.equals(OPRELLESS.getDescricao()) || operacao.equals(OPRELGEQUAL.getDescricao())
                        || operacao.equals(OPRELLEQUAL.getDescricao()) || operacao.equals(OPRELNEQUAL.getDescricao()))
                    return true;
                else
                    return false;
            } else {
                return false;
            }
        } else if (tipo1.equals(REAL.getDescricao())) {
            if (tipo2.equals(REAL.getDescricao()))
                return true;
            if (tipo2.equals(INTEGER.getDescricao()))
                return isCompativel(tipo2, tipo1, operacao);
            else
                return false;
        } else if (tipo1.equals(STRING.getDescricao())) {
            if (tipo2.equals(STRING.getDescricao()))
                return true;
            else
                return false;
        }
        return false;
    }

    private static Boolean isCompativelAtribuicao(String recebe, String tipo) {
        if (recebe.equals(REAL.getDescricao())) {
            return (tipo.equals(REAL.getDescricao()) || tipo.equals(INTEGER.getDescricao()));
        }
        return recebe.equals(tipo);
    }

    private static TokenSintaticosEnum getTipoEnumSintatico(TokenSintatico sintatico) {
        return (TokenSintaticosEnum) sintatico.getTipoToken();
    }

    public static List<Token> analisaDeclaracaoVariaveis(List<Token> listIds, String tipo) throws AnaliseException {
        for (Token t : listIds) {
            String chave = t.getLexema().toUpperCase();
            if (TokenController.getTkIds(chave).getCategoria() != null)
                throw new AnaliseException("Duplicate local variable " + chave, "Semantic");
            TokenController.updateCategoriaTkIds(chave, "VAR");
            TokenController.updateTipoDadoTkIds(chave, tipo);
        }
        listIds.clear();
        return listIds;
    }

    public static List<Token> analisaDeclaracaoVariaveis(List<Token> listIds, String tipo, String chaveProcedure)
            throws AnaliseException {

        TokenProcedure procedure = TokenController.getProcedureLstProcedure(chaveProcedure);
        for (Token t : listIds) {
            String chave = t.getLexema().toUpperCase();
            if ((procedure.getListaIdentificadores().get(chave) != null))
                throw new AnaliseException("Duplicate local variable " + chave, "Semantic");

            TokenIdentificador tokenId = new TokenIdentificador(new Token(ID, chave));
            tokenId.setCategoria("VAR PROCEDURE");
            tokenId.setTipoDado(tipo);
            procedure.getListaIdentificadores().put(chave, tokenId);
        }
        listIds.clear();
        return listIds;
    }

    // private static Boolean isEqualParametros(TokenProcedure procedure, String
    // chave) {
    // for (TokenIdentificador parametro : procedure.getParametros()) {
    // if (parametro.getLexema().toUpperCase().equals(chave))
    // return true;
    // }
    // return false;
    // }

}
