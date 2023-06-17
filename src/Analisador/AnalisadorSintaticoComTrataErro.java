package Analisador;

import java.util.ArrayList;
import java.util.List;

import Token.Token;
import Token.TokenIdentificador;
import Token.TokenSintatico;
import Token.Enums.*;
import static Token.Enums.TokenGenerico.*;
import static Token.Enums.TokensReservados.*;
import static Token.Enums.TokenSintaticosEnum.*;

public class AnalisadorSintaticoComTrataErro {
    private static List<Token> tokensAnalisados = new ArrayList<>();

    public static void start(List<Token> tokens) throws Exception {
        regras(tokens);
        tokensAnalisados.forEach(token -> token.imprimeToken(""));
    }

    private static void regras(List<Token> tokens) throws Exception {
        TokenSintatico programa = new TokenSintatico(BLOCO, new ArrayList<>());
        TokenSintatico bloco = new TokenSintatico(BLOCO, new ArrayList<>());
        TokenSintaticosEnum tipo = PROGRAMA;
        tokensAnalisados = new ArrayList<>();
        try {
            while (!tokens.isEmpty()) {
                switch (tipo) {
                    case PROGRAMA:
                        tokens = analisaPrograma(tokens, programa);
                        tipo = BLOCO;
                        break;
                    case BLOCO:
                        if (comparaToken(tokens.get(0), VAR)) {
                            tipo = DECLARACAO_VARIAVEIS;
                        } else if (comparaToken(tokens.get(0), PROCEDURE)) {
                            tipo = DECLARACAO_PROCEDIMENTO;
                        } else if (comparaToken(tokens.get(0), BEGIN)) {
                            tokens = adicionaTokenAnalisado(tokens, 0, bloco);
                            tipo = COMANDO_COMPOSTO;
                        }
                        break;
                    case DECLARACAO_VARIAVEIS:
                        tokens = analisaDeclaracaoVariaveis(tokens, bloco);
                        tipo = BLOCO;
                        break;
                    case DECLARACAO_PROCEDIMENTO:
                        tokens = analisaDeclaracaoProcedimento(tokens, bloco);
                        tipo = BLOCO;
                        break;
                    case COMANDO_COMPOSTO:
                        tokens = analisaComandoComposto(tokens, 0, List.of(END), bloco);
                        tokens = analisaAdicionaTokensAnalisados(tokens, 0, END, bloco);
                        tokens = analisaAdicionaTokensAnalisados(tokens, 0, FPROGRAM, programa);
                        break;
                    default:
                        tipo = COMANDO_COMPOSTO;
                        break;
                }
            }
            programa.getTokens().add(programa.getTokens().size() - 2, bloco);
        } catch (IndexOutOfBoundsException e) {
            if (programa.getTokens().size() > 1)
                programa.getTokens().add(programa.getTokens().size() - 2, bloco);
            String mensagem = "Syntax error, expecting END of PROGRAM, CONDITIONAL OR REPETITIVE but not found;'";
            programa.getTokens().add(new Token(PROGRAMA, mensagem));
        }
    }

    private static List<Token> trataErro(List<Token> tokens, List<TipoToken> proximosTokenValido) {
        for (Integer i = 0; i < tokens.size(); i++) {
            if (comparaLstToken(tokens.get(i), proximosTokenValido)) {
                return tokens.subList(++i, tokens.size());
            }
        }
        return new ArrayList<>();
    }

    private static Integer trataErroReturnInt(List<Token> tokens, List<TipoToken> proximosTokenValido) {
        for (Integer i = 0; i < tokens.size(); i++) {
            if (comparaLstToken(tokens.get(i), proximosTokenValido)) {
                return ++i;
            }
        }
        return tokens.size();
    }

    private static List<Token> analisaPrograma(List<Token> tokens, TokenSintatico tkPai) {
        List<Token> programa = tkPai.getTokens();
        tokensAnalisados.add(new TokenSintatico(PROGRAMA, programa));

        if (comparaToken(esperaToken(tokens.get(0), PROGRAM, tkPai), ERRO)) {
            return trataErro(tokens.subList(1, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
        } else {
            programa.add(tokens.get(0));
        }
        if (comparaToken(esperaToken(tokens.get(1), ID, tkPai), ERRO)) {
            return trataErro(tokens.subList(2, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
        } else {
            programa.add(tokens.get(1));
            TokenController.updateCategoriaVarTkIds(tokens.get(1).getLexema(), "program");
        }
        if (comparaToken(esperaToken(tokens.get(2), FDEFIN, tkPai), ERRO)) {
            return trataErro(tokens.subList(3, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
        } else {
            programa.add(tokens.get(2));
        }

        return tokens.subList(3, tokens.size());
    }

    private static List<Token> analisaDeclaracaoVariaveis(List<Token> tokens, TokenSintatico tkPai) {
        Integer posDV = 0;
        List<Token> listIds = new ArrayList<>();

        if (comparaToken(esperaToken(tokens.get(posDV++), VAR, tkPai), ERRO))
            return trataErro(tokens.subList(posDV, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));

        while ((!comparaLstToken(tokens.get(posDV), List.of(PROCEDURE, BEGIN)))) {
            if (comparaToken(esperaToken(tokens.get(posDV++), ID, tkPai), ERRO))
                return trataErro(tokens.subList(posDV, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
            else
                listIds.add(TokenController.getTkIds(tokens.get(posDV - 1).getLexema()));
            while (!(comparaToken(tokens.get(posDV), DECTIPO))) {
                if (comparaToken(esperaToken(tokens.get(posDV++), SEPLISTA, tkPai), ERRO))
                    return trataErro(tokens.subList(posDV, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
                if (comparaToken(esperaToken(tokens.get(posDV++), ID, tkPai), ERRO))
                    return trataErro(tokens.subList(posDV, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
                else
                    listIds.add((TokenIdentificador) TokenController.getTkIds(tokens.get(posDV - 1).getLexema()));
            }
            if (comparaToken(esperaToken(tokens.get(posDV++), DECTIPO, tkPai), ERRO))
                return trataErro(tokens.subList(posDV, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
            if (comparaToken(esperaLstToken(tokens.get(posDV++), List.of(INTEGER, REAL, BOOLEAN), tkPai),
                    ERRO))
                return trataErro(tokens.subList(posDV, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
            else {
                Integer pos = posDV - 1;
                listIds.forEach(
                        t -> {
                            String chave = t.getLexema();
                            if (TokenController.getTkIds(chave).getCategoria() != null)
                                TokenController.updateCategoriaVarTkIds(chave, "ERRO");
                            else
                                TokenController.updateCategoriaVarTkIds(chave, "var");
                            String tipo = tokens.get(pos).getLexema();
                            TokenController.updateTipoDadoTkIds(chave, tipo);
                        });
                listIds.clear();
            }
            if (comparaToken(esperaToken(tokens.get(posDV++), FDEFIN, tkPai), ERRO))
                return trataErro(tokens.subList(posDV, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));

        }
        tkPai.getTokens().add(new TokenSintatico(DECLARACAO_VARIAVEIS, tokens.subList(0, posDV)));
        return tokens.subList(posDV, tokens.size());
    }

    private static List<Token> analisaDeclaracaoProcedimento(List<Token> tokens, TokenSintatico tkPai) {
        Integer posDP = 0;

        if (comparaToken(esperaToken(tokens.get(posDP++), PROCEDURE, tkPai), ERRO))
            return trataErro(tokens.subList(posDP, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
        if (comparaToken(esperaToken(tokens.get(posDP++), ID, tkPai), ERRO))
            return trataErro(tokens.subList(posDP, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
        if (comparaToken(tokens.get(posDP), LPAREN)) {
            if (comparaToken(esperaToken(tokens.get(posDP++), LPAREN, tkPai), ERRO))
                return trataErro(tokens.subList(posDP, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
            while ((!comparaToken(tokens.get(posDP), RPAREN))) {
                if (comparaToken(esperaToken(tokens.get(posDP++), VAR, tkPai), ERRO))
                    return trataErro(tokens.subList(posDP, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
                if (comparaToken(esperaToken(tokens.get(posDP++), ID, tkPai), ERRO))
                    return trataErro(tokens.subList(posDP, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
                while (!(comparaToken(tokens.get(posDP), DECTIPO))) {
                    if (comparaToken(esperaToken(tokens.get(posDP++), SEPLISTA, tkPai),
                            ERRO))
                        return trataErro(tokens.subList(posDP, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
                    if (comparaToken(esperaToken(tokens.get(posDP++), ID, tkPai), ERRO))
                        return trataErro(tokens.subList(posDP, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
                }
                if (comparaToken(esperaToken(tokens.get(posDP++), DECTIPO, tkPai), ERRO))
                    return trataErro(tokens.subList(posDP, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
                if (comparaToken(esperaLstToken(tokens.get(posDP++), List.of(INTEGER, REAL, BOOLEAN), tkPai),
                        ERRO))
                    return trataErro(tokens.subList(posDP, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
                if (comparaToken(esperaToken(tokens.get(posDP++), RPAREN, tkPai), ERRO))
                    return trataErro(tokens.subList(posDP, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
            }
        }
        if (comparaToken(esperaToken(tokens.get(posDP++), FDEFIN, tkPai), ERRO))
            return trataErro(tokens.subList(posDP, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
        tkPai.getTokens().add(new TokenSintatico(DECLARACAO_PROCEDIMENTO, tokens.subList(0, posDP)));
        return tokens.subList(posDP, tokens.size());
    }

    private static List<Token> analisaAdicionaTokensAnalisados(List<Token> tokens, Integer pos, TipoToken tkEsperado,
            TokenSintatico tkPai) {
        tkPai.getTokens().add(esperaToken(tokens.get(pos), tkEsperado, tkPai));
        return tokens.subList(pos + 1, tokens.size());
    }

    private static List<Token> analisaCondicional(List<Token> tokens, Integer pos, TokenSintatico tkPai) {
        TokenSintatico tkCondicional = new TokenSintatico(CONDICIONAL, new ArrayList<>());

        tokens = adicionaTokenAnalisado(tokens, pos, tkCondicional);
        tokens = analisaExpressaoTrecho(tokens, List.of(THEN), tkCondicional);
        if (comparaToken(tokens.get(pos), BEGIN)) {
            tokens = adicionaTokenAnalisado(tokens, pos, tkCondicional);
            tokens = analisaComandoComposto(tokens, pos, List.of(END), tkCondicional);
            tokens = adicionaTokenAnalisado(tokens, pos, tkCondicional);
            if (comparaLstToken(tokens.get(pos), List.of(ELSE, FDEFIN)))
                tokens = adicionaTokenAnalisado(tokens, pos, tkCondicional);

        } else {
            tokens = analisaComandoComposto(tokens, pos, List.of(FDEFIN, ELSE), tkCondicional);
            if (comparaToken(tokens.get(pos), FDEFIN))
                if (comparaToken(tokens.get(pos), ELSE))
                    tkCondicional.getTokens().add(new Token(ERRO,
                            "Syntax Error, ';' expected but 'ELSE' found"));
            tokens = adicionaTokenAnalisado(tokens, pos, tkCondicional);
        }
        tkPai.getTokens().add(tkCondicional);
        return tokens;
    }

    private static List<Token> adicionaTokenAnalisado(List<Token> tokens, Integer pos, TokenSintatico tkPai) {
        tkPai.getTokens().add(tokens.get(pos));
        return tokens.subList(pos + 1, tokens.size());
    }

    private static List<Token> analisaRepetitivo(List<Token> tokens, TokenSintatico tkPai) {
        TokenSintatico tkRepetitivo = new TokenSintatico(REPETITIVO, new ArrayList<>());

        if (comparaToken(tokens.get(0), WHILE)) {
            tokens = adicionaTokenAnalisado(tokens, 0, tkRepetitivo);
            tokens = analisaExpressaoTrecho(tokens, List.of(DO), tkRepetitivo);
            if (comparaToken(tokens.get(0), BEGIN)) {
                tokens = adicionaTokenAnalisado(tokens, 0, tkRepetitivo);
                tokens = analisaComandoComposto(tokens, 0, List.of(END), tkRepetitivo);
                tokens = adicionaTokenAnalisado(tokens, 0, tkRepetitivo);// ADICIONA END
            } else {
                tokens = analisaComandoComposto(tokens, 0, List.of(FDEFIN), tkRepetitivo);
            }
        } else if (comparaToken(tokens.get(0), REPEAT)) {
            tokens = adicionaTokenAnalisado(tokens, 0, tkRepetitivo);
            tokens = analisaComandoComposto(tokens, 0, List.of(UNTIL), tkRepetitivo);
            tokens = adicionaTokenAnalisado(tokens, 0, tkRepetitivo);// ADICIONA UNTIL
            tokens = analisaExpressaoTrecho(tokens, List.of(FDEFIN), tkRepetitivo);
        } else if (comparaToken(tokens.get(0), FOR)) {
            // tokens = adicionaTokenAnalisado(tokens, 0); FOR
            // tokensAnalisados.add(esperaToken(tokens.get(0)), ID); ID
            // tokensAnalisados.add(esperaToken(tokens.get(1)), ATRIBUICAO); ATRIBUICAO
            // EXPRESSAO
            // TO
            // EXPRESSAO
            // DO
            // BEGIN
            // COMANDOS
            // END
        }
        tkPai.getTokens().add(tkRepetitivo);
        return tokens;
    }

    private static List<Token> analisaComandoComposto(List<Token> tokens, Integer pos, List<TipoToken> finalExpressao,
            TokenSintatico tkPai) {
        TokenSintatico comandoComposto = new TokenSintatico(COMANDO_COMPOSTO, new ArrayList<>());

        while (!comparaLstToken(tokens.get(pos), finalExpressao)) {
            pos = 0;
            if (comparaToken(tokens.get(pos), ID)) {
                comandoComposto.getTokens().add(tokens.get(pos));
                if (comparaLstToken(tokens.get(++pos), List.of(FDEFIN, LPAREN))) {
                    tokens = analisaChamadaProcedimento(tokens, tkPai);
                } else {
                    if (comparaToken(esperaToken(tokens.get(pos), ATRIBUICAO, comandoComposto), ERRO)) {
                        comandoComposto.getTokens().remove(tokens.get(pos - 1));
                        tokens = trataErro(tokens, List.of(FDEFIN));
                    } else {
                        comandoComposto.getTokens().add(tokens.get(pos));
                        tokens = tokens.subList(++pos, tokens.size());
                        tokens = analisaExpressaoTrecho(tokens, List.of(FDEFIN), comandoComposto);
                    }
                }
            } else if (comparaToken(tokens.get(pos), IF)) {
                tokens = analisaCondicional(tokens, pos, comandoComposto);
            } else if (comparaLstToken(tokens.get(pos), List.of(WHILE, REPEAT, FOR))) {
                tokens = analisaRepetitivo(tokens, comandoComposto);
            } else {
                if (comparaToken(esperaLstToken(tokens.get(pos), finalExpressao, tkPai), ERRO))
                    return trataErro(tokens.subList(pos, tokens.size()), List.of(BEGIN, PROCEDURE, VAR));
            }
        }
        tkPai.getTokens().add(comandoComposto);
        return tokens;
    }

    private static List<Token> analisaChamadaProcedimento(List<Token> tokens, TokenSintatico tkPai) {
        Integer pos = 0;
        TokenSintatico chamadaProcedimento = new TokenSintatico(CHAMADA_PROCEDIMENTO, new ArrayList<>());

        chamadaProcedimento.getTokens().add(tokens.get(pos++));
        if (comparaToken(tokens.get(pos), LPAREN)) {
            chamadaProcedimento.getTokens().add(tokens.get(pos++));
            if (comparaToken(tokens.get(pos), RPAREN)) {
                chamadaProcedimento.getTokens().add(tokens.get(pos++));
            } else {
                while (!comparaToken(tokens.get(pos), SEPLISTA)) {
                    List<Token> tokensAux = tokens;
                    List<TipoToken> lst = List.of(RPAREN, SEPLISTA);
                    tokens = analisaExpressaoTrecho(tokens.subList(pos, tokens.size()), lst, chamadaProcedimento);
                    tokensAux = tokensAux.subList(tokensAux.size() - tokens.size() - 1, tokens.size());
                    if (comparaToken(tokensAux.get(0), RPAREN)) {
                        pos = 0;
                        chamadaProcedimento.getTokens().add(tokensAux.get(pos));
                        break;
                    }
                }
            }
        }
        if (comparaToken(esperaToken(tokens.get(pos), FDEFIN, tkPai), ERRO))
            return trataErro(tokens.subList(pos, tokens.size()), List.of(FDEFIN));
        chamadaProcedimento.getTokens().add(tokens.get(pos));
        tkPai.getTokens().add(chamadaProcedimento);
        return tokens.subList(++pos, tokens.size());
    }

    private static List<Token> analisaExpressaoTrecho(List<Token> tokens, List<TipoToken> finalExpressoes,
            TokenSintatico tkPai) {
        Integer posE = 0, posAux;
        TokenSintatico lstExp = new TokenSintatico(LISTA_EXPRESSAO, new ArrayList<>());
        List<TipoToken> listaCmp = List.of(ID, VALORINT, VALORLIT, VALOREAL, OPARITSOMA, OPARITSUB, OPALOGNOT, LPAREN);
        if (comparaToken(esperaLstToken(tokens.get(posE), listaCmp, tkPai), ERRO))
            return trataErro(tokens.subList(posE, tokens.size()), finalExpressoes);
        while (!comparaLstToken(tokens.get(posE), finalExpressoes)) {
            posAux = posE;
            posE = analisaExpressao(tokens, posE, finalExpressoes, lstExp);
            lstExp.getTokens().add(new TokenSintatico(EXPRESSAO, tokens.subList(posAux, posE)));
        }
        tkPai.getTokens().add(lstExp);
        return tokens.subList(++posE, tokens.size());
    }

    private static Integer analisaExpressao(List<Token> tokens, Integer pos, List<TipoToken> finalExpressoes,
            TokenSintatico tkPai) {
        if (comparaToken(tokens.get(pos), LPAREN)) {

            List<TipoToken> listaCmp = new ArrayList<>(List.of(ID, VALORINT, VALORLIT, VALOREAL));
            listaCmp.addAll(List.of(OPARITSOMA, OPARITSUB, OPALOGNOT, LPAREN));
            if (comparaToken(esperaLstToken(tokens.get(++pos), listaCmp, tkPai), ERRO))
                return trataErroReturnInt(tokens.subList(pos, tokens.size()), finalExpressoes);
            pos = analisaExpressao(tokens, pos, List.of(RPAREN), tkPai);
            while (!comparaToken(tokens.get(pos), RPAREN)) {
                pos = analisaExpressao(tokens, pos, List.of(RPAREN), tkPai);
            }
            if (comparaToken(esperaLstToken(tokens.get(++pos), finalExpressoes, tkPai), ERRO))
                return trataErroReturnInt(tokens.subList(pos, tokens.size()), finalExpressoes);

        } else if (comparaLstToken(tokens.get(pos),
                List.of(OPARITSOMA, OPARITSUB, OPARITMULT, OPALOGNOT, OPARITDIV, OPARITMOD))) {
            List<TipoToken> listaCmp = List.of(ID, VALORLIT, VALORINT, VALOREAL, OPALOGNOT, LPAREN);
            if (comparaToken(esperaLstToken(tokens.get(++pos), listaCmp, tkPai), ERRO))
                return trataErroReturnInt(tokens.subList(pos, tokens.size()), finalExpressoes);

        } else if (comparaLstToken(tokens.get(pos), List.of(ID, VALORINT, VALOREAL))) {

            List<TipoToken> listaTokensEsperados = new ArrayList<>(List.of(OPRELGREAT, OPRELLESS, OPRELEQUAL));
            listaTokensEsperados.addAll(List.of(OPRELGEQUAL, OPRELLEQUAL, OPRELNEQUAL, OPALOGOR, OPALOGAND, OPALOGNOT));
            listaTokensEsperados.addAll(List.of(OPARITSOMA, OPARITSUB, OPARITMULT, OPARITDIV, OPARITMOD));
            listaTokensEsperados.addAll(finalExpressoes);

            if (comparaToken(esperaLstToken(tokens.get(++pos), listaTokensEsperados, tkPai), ERRO))
                return trataErroReturnInt(tokens.subList(pos, tokens.size()), finalExpressoes);

            if (comparaLstToken(tokens.get(pos),
                    List.of(OPRELEQUAL, OPRELNEQUAL, OPRELLESS, OPRELLEQUAL, OPRELGEQUAL, OPRELGREAT))) {
                List<TipoToken> listaCmp = List.of(ID, VALORINT, VALOREAL, OPARITSOMA, OPARITSUB, OPALOGNOT, LPAREN);
                if (comparaToken(esperaLstToken(tokens.get(++pos), listaCmp, tkPai), ERRO))
                    return trataErroReturnInt(tokens.subList(pos, tokens.size()), finalExpressoes);
                analisaExpressao(tokens, pos, finalExpressoes, tkPai);
                return pos;
            }
        } else if (comparaToken(tokens.get(pos), VALORLIT)) {
            List<TipoToken> listaTokensEsperados = new ArrayList<>(List.of(OPARITSOMA));
            listaTokensEsperados.addAll(finalExpressoes);

            if (comparaToken(esperaLstToken(tokens.get(++pos), listaTokensEsperados, tkPai), ERRO))
                return trataErroReturnInt(tokens.subList(pos, tokens.size()), finalExpressoes);
        }
        return pos;
    }

    private static Token esperaLstToken(Token tokenRecebido, List<TipoToken> tokensEsperados, TokenSintatico tkPai) {
        String mensagem = "Syntax error, not expecting '" + tokenRecebido.getTipo().getDescricao() + "'";
        for (TipoToken tipoToken : tokensEsperados) {
            if (tokenRecebido.getTipo().equals(tipoToken)) {
                return tokenRecebido;
            }
        }

        tkPai.getTokens().add(new Token(ERRO, mensagem));
        return new Token(ERRO, mensagem);
    }

    private static Token esperaToken(Token tokenRecebido, TipoToken tokenEsperado, TokenSintatico tkPai) {
        String mensagem = "Syntax error, expecting '" + tokenEsperado.getDescricao() + "' ";
        mensagem += "but '" + tokenRecebido.getTipo().getDescricao() + "' found";

        if (tokenRecebido.getTipo().equals(tokenEsperado))
            return tokenRecebido;

        tkPai.getTokens().add(new Token(ERRO, mensagem));
        return new Token(ERRO, mensagem);
    }

    private static boolean comparaLstToken(Token tokenRecebido, List<TipoToken> tokensEsperados) {
        for (TipoToken tipoToken : tokensEsperados) {
            if (tokenRecebido.getTipo().equals(tipoToken)) {
                return true;
            }
        }
        return false;
    }

    private static boolean comparaToken(Token tokenRecebido, TipoToken tokenEsperado) {
        return tokenRecebido.getTipo().equals(tokenEsperado);
    }

}
