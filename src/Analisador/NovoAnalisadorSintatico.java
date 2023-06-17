package Analisador;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import Exceptions.SyntaxException;
import Token.Token;
import Token.TokenIdentificador;
import Token.TokenSintatico;
import Token.Enums.*;
import static Token.Enums.TokenGenerico.*;
import static Token.Enums.TokensReservados.*;
import static Token.Enums.TokenSintaticosEnum.*;

//SUBSTITUIR .getTokens().add(tokenAtual); POR ADICIONATOKENANALISADO()
public class NovoAnalisadorSintatico {

    private static List<Token> tokensAnalisados = new ArrayList<>();
    private static TokenSintatico programa = new TokenSintatico(PROGRAMA, new ArrayList<>());
    private static Stack<Token> guardaTokensObtidos = new Stack<>();
    private static TokenSintaticosEnum estadoAnalise;
    private static Token tokenAtual;

    public static void start() {
        estadoAnalise = PROGRAMA;
        try {
            tokenAtual = geraToken();
            regras();
        } catch (SyntaxException e) {
            System.out.println("\u001B[31m" + e.getMessage() + "\u001B[0m");
        }
        tokensAnalisados.forEach(token -> token.imprimeToken(""));

    }

    private static void regras() throws SyntaxException {

        TokenSintatico bloco = new TokenSintatico(BLOCO, new ArrayList<>());

        try {
            while (estadoAnalise != FIM_PROGRAMA) {
                switch (estadoAnalise) {
                    case PROGRAMA:
                        analisaPrograma();
                        break;
                    case BLOCO:
                        if (comparaToken(tokenAtual, VAR)) {
                            estadoAnalise = DECLARACAO_VARIAVEIS;
                        } else if (comparaToken(tokenAtual, PROCEDURE)) {
                            estadoAnalise = DECLARACAO_PROCEDIMENTO;
                        } else if (comparaToken(tokenAtual, BEGIN)) {
                            addTokenAnalisado(bloco);
                            estadoAnalise = COMANDO_COMPOSTO;
                        } else {
                            bloco.getTokens().add(new Token(ERRO,
                                    "Syntax exception, " + tokenAtual.getLexema()
                                            + " is not a valid token for <BLOCO>"));
                            geraToken();
                        }
                        break;
                    case DECLARACAO_VARIAVEIS:
                        analisaDeclaracaoVariaveis(bloco);
                        break;
                    case DECLARACAO_PROCEDIMENTO:
                        analisaDeclaracaoProcedimento(bloco);
                        break;
                    case COMANDO_COMPOSTO:
                        analisaComandoComposto(List.of(END), bloco);
                        esperaToken(tokenAtual, END, bloco);
                        esperaToken(geraToken(), FPROGRAM, programa);
                        estadoAnalise = FIM_PROGRAMA;
                        break;
                    default:
                        estadoAnalise = COMANDO_COMPOSTO;
                        break;
                }
            }
        } catch (SyntaxException e) {
            throw e;
        } finally {
            programa.getTokens().add(programa.getTokens().size() - 1, bloco);
        }

    }

    private static Boolean analisaPrograma() throws SyntaxException {
        tokensAnalisados.add(programa);

        if (!esperaToken(tokenAtual, PROGRAM, programa)) {
            return trataErro(List.of(BEGIN, PROCEDURE, VAR));
        }
        if (!esperaToken(geraToken(), ID, programa)) {
            return trataErro(List.of(BEGIN, PROCEDURE, VAR));
        } else {
            TokenController.updateCategoriaVarTkIds(tokenAtual.getLexema(), "program");
        }
        if (!esperaToken(geraToken(), FDEFIN, programa)) {
            return trataErro(List.of(BEGIN, PROCEDURE, VAR));
        }
        estadoAnalise = BLOCO;
        geraToken();
        return true;
    }

    private static Boolean analisaDeclaracaoVariaveis(TokenSintatico tkPai) throws SyntaxException {
        List<Token> listIds = new ArrayList<>();
        TokenSintatico declaracaoV = new TokenSintatico(DECLARACAO_VARIAVEIS, new ArrayList<>());

        if (!esperaToken(tokenAtual, VAR, declaracaoV))
            return trataErro(List.of(BEGIN, PROCEDURE, VAR));

        while (comparaLstToken(geraToken(), List.of(PROCEDURE, BEGIN)) == false) {
            if (!esperaToken(tokenAtual, ID, declaracaoV))
                return trataErro(List.of(BEGIN, PROCEDURE, VAR));
            listIds.add(TokenController.getTkIds(tokenAtual.getLexema()));

            while (comparaToken(geraToken(), DECTIPO) == false) {
                if (!esperaToken(tokenAtual, SEPLISTA, declaracaoV))
                    return trataErro(List.of(BEGIN, PROCEDURE, VAR));
                if (!esperaToken(geraToken(), ID, declaracaoV))
                    return trataErro(List.of(BEGIN, PROCEDURE, VAR));
                listIds.add((TokenIdentificador) TokenController.getTkIds(tokenAtual.getLexema()));
            }

            if (!esperaToken(tokenAtual, DECTIPO, declaracaoV))
                return trataErro(List.of(BEGIN, PROCEDURE, VAR));
            if (!esperaLstToken(geraToken(), List.of(INTEGER, REAL, BOOLEAN, STRING), declaracaoV))
                return trataErro(List.of(BEGIN, PROCEDURE, VAR));
            {
                // ANALISADOR SEMANTICO AQUI
            }
            if (!esperaToken(geraToken(), FDEFIN, declaracaoV))
                return trataErro(List.of(BEGIN, PROCEDURE, VAR));
        }
        tkPai.getTokens().add(declaracaoV);
        estadoAnalise = BLOCO;
        return true;
    }

    private static Boolean analisaDeclaracaoProcedimento(TokenSintatico tkPai) throws SyntaxException {
        TokenSintatico declaracaoP = new TokenSintatico(DECLARACAO_PROCEDIMENTO, new ArrayList<>());

        if (!esperaToken(tokenAtual, PROCEDURE, declaracaoP))
            return trataErro(List.of(BEGIN, PROCEDURE, VAR));
        if (!esperaToken(geraToken(), ID, declaracaoP))
            return trataErro(List.of(BEGIN, PROCEDURE, VAR));

        if (comparaToken(geraToken(), LPAREN)) {
            TokenSintatico parametrosFormais = new TokenSintatico(PARAMETROS_FORMAIS, new ArrayList<>());
            declaracaoP.getTokens().add(parametrosFormais);
            addTokenAnalisado(parametrosFormais);

            while ((!comparaToken(tokenAtual, RPAREN))) {
                if (comparaToken(geraToken(), VAR)) {
                    addTokenAnalisado(parametrosFormais);
                    geraToken();
                }
                if (!esperaToken(tokenAtual, ID, parametrosFormais))
                    return trataErro(List.of(BEGIN, PROCEDURE, VAR));

                while (!(comparaToken(geraToken(), DECTIPO))) {
                    if (!esperaToken(tokenAtual, SEPLISTA, parametrosFormais))
                        return trataErro(List.of(BEGIN, PROCEDURE, VAR));
                    if (!esperaToken(geraToken(), ID, parametrosFormais))
                        return trataErro(List.of(BEGIN, PROCEDURE, VAR));
                }

                if (!esperaToken(tokenAtual, DECTIPO, parametrosFormais))
                    return trataErro(List.of(BEGIN, PROCEDURE, VAR));
                if (!esperaLstToken(geraToken(), List.of(INTEGER, REAL, BOOLEAN, STRING), parametrosFormais))
                    return trataErro(List.of(BEGIN, PROCEDURE, VAR));
                if (!esperaLstToken(geraToken(), List.of(FDEFIN, RPAREN), parametrosFormais))
                    return trataErro(List.of(BEGIN, PROCEDURE, VAR));
            }

        }
        if (!esperaToken(geraToken(), FDEFIN, declaracaoP))
            return trataErro(List.of(BEGIN, PROCEDURE, VAR));

        if (comparaToken(geraToken(), VAR)) {
            analisaDeclaracaoVariaveis(declaracaoP);
        }

        if (!esperaToken(tokenAtual, BEGIN, declaracaoP))
            return trataErro(List.of(BEGIN, PROCEDURE, VAR));

        analisaComandoComposto(List.of(END), declaracaoP);
        addTokenAnalisado(declaracaoP);

        if (!esperaToken(geraToken(), FDEFIN, declaracaoP))
            return trataErro(List.of(BEGIN, PROCEDURE, VAR));

        geraToken();
        tkPai.getTokens().add(declaracaoP);
        return true;
    }

    private static Boolean analisaComandoComposto(List<TipoToken> finalExpressao, TokenSintatico tkPai)
            throws SyntaxException {

        TokenSintatico comandoComposto = new TokenSintatico(COMANDO_COMPOSTO, new ArrayList<>());
        while (comparaLstToken(geraToken(), finalExpressao) == false) {
            analisaComando(finalExpressao, comandoComposto);
        }

        tkPai.getTokens().add(comandoComposto);
        return true;
    }

    private static List<TipoToken> proceduresReservadas() {
        return List.of(WRITE, WRITELN, READ, READLN, PRINT);
    }

    private static Boolean analisaComando(List<TipoToken> finalExpressao, TokenSintatico tkPai) throws SyntaxException {
        TokenSintatico comando = new TokenSintatico(COMANDO, new ArrayList<>());
        List<TipoToken> lstTksProced = proceduresReservadas();

        if (comparaToken(tokenAtual, ID) || comparaLstToken(tokenAtual, lstTksProced)) {
            comando.getTokens().add(tokenAtual);
            if (comparaLstToken(geraToken(), List.of(FDEFIN, LPAREN))) {
                List<TipoToken> fimExpressao = finalExpressao;
                if (tkPai.getTipo().equals(COMANDO_COMPOSTO))
                    fimExpressao = List.of(FDEFIN);
                analisaChamadaProcedimento(comando, fimExpressao);
            } else {
                if (!esperaLstToken(tokenAtual, List.of(ATRIBUICAO), comando)) {
                    comando.getTokens().remove(comando.getTokens().size() - 2);
                    trataErro(List.of(FDEFIN));
                } else {
                    List<TipoToken> fimExpressao = finalExpressao;
                    if (tkPai.getTipo().equals(COMANDO_COMPOSTO))
                        fimExpressao = List.of(FDEFIN);
                    analisaExpressaoTrecho(fimExpressao, comando);
                }
            }
        } else if (comparaToken(tokenAtual, IF)) {
            analisaCondicional(comando, finalExpressao);
        } else if (comparaLstToken(tokenAtual, List.of(WHILE, REPEAT, FOR))) {
            analisaRepetitivo(comando);
        } else {
            if (!esperaLstToken(tokenAtual, finalExpressao, tkPai))
                return trataErro(List.of(FDEFIN));
        }

        tkPai.getTokens().add(comando);
        return true;
    }

    private static Boolean analisaChamadaProcedimento(TokenSintatico tkPai, List<TipoToken> finalExpressao)
            throws SyntaxException {
        TokenSintatico chamadaProcedimento = new TokenSintatico(CHAMADA_PROCEDIMENTO, new ArrayList<>());

        chamadaProcedimento.getTokens().add(tkPai.getTokens().remove(tkPai.getTokens().size() - 1));

        if (comparaToken(tokenAtual, LPAREN)) {
            chamadaProcedimento.getTokens().add(tokenAtual);
            if (comparaToken(geraToken(), RPAREN)) {
                chamadaProcedimento.getTokens().add(tokenAtual);
            } else {
                voltaToken();
                while (!comparaToken(tokenAtual, RPAREN)) {
                    analisaExpressaoTrecho(List.of(RPAREN, SEPLISTA), chamadaProcedimento);
                }
                if (comparaToken(tokenAtual, RPAREN)) {
                    // List<Token> listaTk = chamadaProcedimento.getTokens();
                    // Token lParen = listaTk.remove(1);
                    // ((TokenSintatico) listaTk.get(1)).getTokens().add(0, lParen);

                    List<Token> listaLstExpr = new ArrayList<>();
                    List<Token> removeAll = new ArrayList<>();

                    listaLstExpr.add(chamadaProcedimento.getTokens().remove(1));
                    for (Token t : chamadaProcedimento.getTokens()) {
                        if (t.getTipo().equals(LISTA_EXPRESSAO)) {
                            listaLstExpr.addAll(((TokenSintatico) t).getTokens());
                            removeAll.add(t);
                        }
                    }

                    chamadaProcedimento.getTokens().removeAll(removeAll);
                    chamadaProcedimento.getTokens().add(new TokenSintatico(LISTA_EXPRESSAO, listaLstExpr));
                }
            }
            geraToken();
        }
        if (!esperaLstToken(tokenAtual, finalExpressao, chamadaProcedimento))
            return trataErro(List.of(FDEFIN));
        tkPai.getTokens().add(chamadaProcedimento);
        return true;
    }

    private static Boolean analisaExpressaoTrecho(List<TipoToken> fExpressoes, TokenSintatico tkPai)
            throws SyntaxException {

        TokenSintatico lstExp = new TokenSintatico(LISTA_EXPRESSAO, new ArrayList<>());
        List<TipoToken> listaCmp = List.of(ID, VALORINT, VALORLIT, VALOREAL, OPARITSOMA, OPARITSUB, OPALOGNOT, LPAREN);

        if (!esperaLstToken(geraToken(), listaCmp, lstExp))
            return trataErro(fExpressoes);

        Integer qtd = 0;
        while (!comparaLstToken(tokenAtual, fExpressoes)) {
            analisaExpressao(fExpressoes, lstExp, qtd++);
        }

        tkPai.getTokens().add(lstExp);
        return true;
    }

    private static List<TipoToken> concArray(List<TipoToken> array1, List<TipoToken> array2) {
        List<TipoToken> retorno = array1;
        retorno.addAll(array2);
        return retorno;
    }

    private static Boolean analisaExpressao(List<TipoToken> finalExpressoes, TokenSintatico tkPai, Integer qtd)
            throws SyntaxException {

        List<TipoToken> opArit = new ArrayList<>();
        List<TipoToken> opRela = new ArrayList<>();
        List<TipoToken> opLogic = new ArrayList<>();
        List<TipoToken> operadores = new ArrayList<>();
        opArit.addAll(List.of(OPARITSOMA, OPARITSUB, OPARITMULT, OPARITDIV, OPARITDIVINT, OPARITMOD));
        opRela.addAll(List.of(OPRELGREAT, OPRELLESS, OPRELEQUAL, OPRELGEQUAL, OPRELLEQUAL, OPRELNEQUAL));
        opLogic.addAll(List.of(OPALOGOR, OPALOGAND, OPALOGNOT));
        operadores.addAll(concArray(concArray(opArit, opLogic), opRela));

        if (comparaToken(tokenAtual, LPAREN)) {

            List<TipoToken> listaTokensEsperados = new ArrayList<>(List.of(ID, VALORINT, VALORLIT, VALOREAL));
            listaTokensEsperados.addAll(List.of(OPARITSOMA, OPARITSUB, OPALOGNOT, LPAREN));

            if (!esperaLstToken(geraToken(), listaTokensEsperados, tkPai))
                return trataErro(finalExpressoes);
            analisaExpressao(List.of(RPAREN), tkPai, qtd);
            while (!comparaToken(tokenAtual, RPAREN)) {
                analisaExpressao(List.of(RPAREN), tkPai, qtd);
            }
            listaTokensEsperados.clear();
            listaTokensEsperados.addAll(operadores);
            listaTokensEsperados.addAll(finalExpressoes);
            if (!esperaLstToken(geraToken(), listaTokensEsperados, tkPai))
                return trataErro(finalExpressoes);

        } else if (comparaLstToken(tokenAtual, concArray(opArit, List.of(OPALOGNOT)))
                || (comparaLstToken(tokenAtual, concArray(opLogic, List.of(OPALOGOR, OPALOGAND))) && (qtd > 0))) {

            List<TipoToken> listaCmp = List.of(ID, VALORLIT, VALORINT, VALOREAL, OPALOGNOT, LPAREN);
            if (!esperaLstToken(geraToken(), listaCmp, tkPai))
                return trataErro(finalExpressoes);

        } else if (comparaLstToken(tokenAtual, List.of(ID, VALORINT, VALOREAL))) {

            List<TipoToken> listaTokensEsperados = operadores;
            listaTokensEsperados.addAll(finalExpressoes);

            if (!esperaLstToken(geraToken(), listaTokensEsperados, tkPai))
                return trataErro(finalExpressoes);
            if (comparaLstToken(tokenAtual, opRela)) {
                List<TipoToken> listaCmp = new ArrayList<>();
                listaCmp.addAll(List.of(ID, VALORINT, VALOREAL, VALORLIT, OPARITSOMA, OPARITSUB, OPALOGNOT, LPAREN));
                if (!esperaLstToken(geraToken(), listaCmp, tkPai))
                    return trataErro(finalExpressoes);
                analisaExpressao(finalExpressoes, tkPai, qtd);
                return true;
            }

        } else if (comparaToken(tokenAtual, VALORLIT)) {

            List<TipoToken> listaTokensEsperados = new ArrayList<>(List.of(OPARITSOMA, OPRELEQUAL, OPRELNEQUAL));
            listaTokensEsperados.addAll(finalExpressoes);
            geraToken();
            if (!esperaLstToken(tokenAtual, listaTokensEsperados, tkPai))
                return trataErro(finalExpressoes);
        }
        return true;
    }

    private static void removerUltimoTokenLstExpressao(TokenSintatico retirarDe, TipoToken TIPO) {
        Token ultimaExpressaoAnalisada = retirarDe.getTokens().get(retirarDe.getTokens().size() - 1);
        if (ultimaExpressaoAnalisada.getTipo().equals(LISTA_EXPRESSAO)) {
            List<Token> lstTokensExpressao = ((TokenSintatico) ultimaExpressaoAnalisada).getTokens();
            if (lstTokensExpressao.get(lstTokensExpressao.size() - 1).getTipo().equals(TIPO))
                lstTokensExpressao.remove(lstTokensExpressao.size() - 1);
        }
    }

    private static Boolean analisaRepetitivo(TokenSintatico tkPai) throws SyntaxException {
        TokenSintatico tkRepetitivo = new TokenSintatico(REPETITIVO, new ArrayList<>());
        tkRepetitivo.getTokens().add(tokenAtual);

        if (comparaToken(tokenAtual, WHILE)) {
            analisaExpressaoTrecho(List.of(DO), tkRepetitivo);

            removerUltimoTokenLstExpressao(tkRepetitivo, DO);

            tkRepetitivo.getTokens().add(tokenAtual);
            if (comparaToken(geraToken(), BEGIN)) {
                tkRepetitivo.getTokens().add(tokenAtual);
                analisaComandoComposto(List.of(END), tkRepetitivo);
                tkRepetitivo.getTokens().add(tokenAtual);
                if (!esperaToken(geraToken(), FDEFIN, tkRepetitivo)) {
                    return trataErro(List.of(FDEFIN));
                }
            } else {
                analisaComando(List.of(FDEFIN), tkRepetitivo);
            }
        } else if (comparaToken(tokenAtual, REPEAT)) {
            analisaComandoComposto(List.of(UNTIL), tkRepetitivo);
            tkRepetitivo.getTokens().add(tokenAtual);
            analisaExpressaoTrecho(List.of(FDEFIN), tkRepetitivo);
        } else if (comparaToken(tokenAtual, FOR)) {
            if (!esperaToken(geraToken(), ID, tkRepetitivo))
                return trataErro(List.of(FDEFIN));
            if (!esperaToken(geraToken(), ATRIBUICAO, tkRepetitivo))
                return trataErro(List.of(FDEFIN));

            analisaExpressaoTrecho(List.of(TO, DOWNTO), tkRepetitivo);

            removerUltimoTokenLstExpressao(tkRepetitivo, tokenAtual.getTipo());
            if (!esperaLstToken(tokenAtual, List.of(TO, DOWNTO), tkRepetitivo))
                return trataErro(List.of(FDEFIN));

            analisaExpressaoTrecho(List.of(DO), tkRepetitivo);

            removerUltimoTokenLstExpressao(tkRepetitivo, DO);
            if (!esperaToken(tokenAtual, DO, tkRepetitivo))
                return trataErro(List.of(FDEFIN));
            if (!esperaToken(geraToken(), BEGIN, tkRepetitivo))
                return trataErro(List.of(FDEFIN));
            analisaComandoComposto(List.of(END), tkRepetitivo);
            if (!esperaToken(tokenAtual, END, tkRepetitivo))
                return trataErro(List.of(FDEFIN));
            if (!esperaToken(geraToken(), FDEFIN, tkRepetitivo))
                return trataErro(List.of(FDEFIN));
        }
        tkPai.getTokens().add(tkRepetitivo);
        return true;
    }

    private static Boolean analisaCondicional(TokenSintatico tkPai, List<TipoToken> finalExpressao)
            throws SyntaxException {

        TokenSintatico tkCondicional = new TokenSintatico(CONDICIONAL, new ArrayList<>());
        List<Token> tokensCondicionais = tkCondicional.getTokens();

        tokensCondicionais.add(tokenAtual);
        analisaExpressaoTrecho(List.of(THEN), tkCondicional);
        removerUltimoTokenLstExpressao(tkCondicional, THEN);
        tokensCondicionais.add(tokenAtual);

        if (comparaToken(geraToken(), BEGIN)) {
            tokensCondicionais.add(tokenAtual);
            analisaComandoComposto(List.of(END), tkCondicional);
            tokensCondicionais.add(tokenAtual);
        } else {
            analisaComando(List.of(FDEFIN, ELSE), tkCondicional);
            voltaToken();
        }

        if (comparaToken(geraToken(), ELSE)) {
            tokensCondicionais.add(tokenAtual);
            if (comparaToken(geraToken(), BEGIN)) {
                analisaComandoComposto(List.of(END), tkCondicional);
                if (!esperaToken(geraToken(), FDEFIN, tkCondicional))
                    return trataErro(List.of(FDEFIN));
            } else {
                analisaComando(List.of(FDEFIN), tkCondicional);
            }
        } else if (comparaToken(tokenAtual, FDEFIN)) {
            tokensCondicionais.add(tokenAtual);
        } else {
            tokensCondicionais.add(new Token(ERRO, "Syntax Error, expecting ';' after end"));
            voltaToken();
        }

        tkPai.getTokens().add(tkCondicional);
        return true;
    }

    private static Boolean trataErro(List<TipoToken> proximosTokenValido) throws SyntaxException {
        String tokenComErro = tokenAtual.getTipo().getDescricao();
        System.out.println("\u001B[31m ----------- ERRO ------------ \u001B[0m");
        while (geraToken() != null) {
            if (comparaLstToken(tokenAtual, proximosTokenValido)) {
                return false;
            }
        }
        throw new SyntaxException("Syntax Exception, is not possible to recover from token " + tokenComErro);
    }

    private static boolean comparaToken(Token tokenRecebido, TipoToken tokenEsperado) {
        return tokenRecebido.getTipo().equals(tokenEsperado);
    }

    private static boolean comparaLstToken(Token tokenRecebido, List<TipoToken> tokensEsperados) {

        for (TipoToken tipoToken : tokensEsperados) {
            if (tokenRecebido.getTipo().equals(tipoToken)) {
                return true;
            }
        }
        return false;
    }

    private static Boolean esperaToken(Token tokenRecebido, TipoToken tokenEsperado, TokenSintatico tkPai)
            throws SyntaxException {

        String mensagem = "Syntax error, expecting '" + tokenEsperado.getDescricao() + "' ";
        mensagem += "but '" + tokenRecebido.getTipo().getDescricao() + "' found";

        if (tokenRecebido.getTipo().equals(ERRO)) {
            mensagem = tokenRecebido.getLexema();
            tkPai.getTokens().add(tokenRecebido);
            return false;
        }

        Boolean isCorreto = tokenRecebido.getTipo().equals(tokenEsperado);
        Token esperaToken = isCorreto ? tokenRecebido : new Token(ERRO, mensagem);
        tkPai.getTokens().add(esperaToken);

        return isCorreto;
    }

    private static Boolean esperaLstToken(Token tokenRecebido, List<TipoToken> tokensEsperados, TokenSintatico tkPai) {
        String mensagem = "Syntax error, not expecting '" + tokenRecebido.getTipo().getDescricao() + "'";

        if (tokenRecebido.getTipo().equals(ERRO)) {
            mensagem = tokenRecebido.getLexema();
            tkPai.getTokens().add(tokenRecebido);
            return false;
        }

        for (TipoToken tipoToken : tokensEsperados) {
            if (tokenRecebido.getTipo().equals(tipoToken)) {
                tkPai.getTokens().add(tokenRecebido);
                return true;
            }
        }

        tkPai.getTokens().add(new Token(ERRO, mensagem));
        return false;
    }

    private static void addTokenAnalisado(TokenSintatico tkPai) {
        tkPai.getTokens().add(tokenAtual);
    }

    private static Token geraToken() throws SyntaxException {
        guardaTokensObtidos.push(tokenAtual);
        tokenAtual = AnalisadorLexico.proximoToken();
        while (tokenAtual == null
                || comparaLstToken(tokenAtual, List.of(COMENTLIN, LCOMENTK, LCOMENTP, RCOMENTK, RCOMENTP))) {
            tokenAtual = AnalisadorLexico.proximoToken();
        }

        if (tokenAtual.getTipo().equals(FIMLEITURA)) {
            String mensagem = "was no possible to find the end of program, token chain ended before.";
            programa.getTokens().add(new Token(ERRO, mensagem));
            throw new SyntaxException("");
        }

        System.out.println("-------------------------------------------");
        System.out.println(tokenAtual);

        return tokenAtual;
    }

    private static Token voltaToken() {
        AnalisadorLexico.pushToken(tokenAtual);
        tokenAtual = guardaTokensObtidos.pop();

        System.out.println("----------------VOLTA----------------------");
        System.out.println(tokenAtual);

        return tokenAtual;
    }
}
