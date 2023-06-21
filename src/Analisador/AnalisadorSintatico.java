package Analisador;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import Exceptions.AnaliseException;
import Token.Token;
import Token.TokenIdentificador;
import Token.TokenProcedure;
import Token.TokenSintatico;
import Token.Enums.*;
import static Token.Enums.TokenGenerico.*;
import static Token.Enums.TokensReservados.*;
import static Token.Enums.TokenSintaticosEnum.*;

//DECLARACAO DE PROCEDIMENTOS NAO EXECUTA COMO ESPERADO
//PROCEDIMENTO, PARAMETROS NAO PODE TER O MESMO NOME QUE AS VARIAVEIS DA PROCEDURE
//AO INVES DE ADICIONAR A DECLARAO PROCEDURE NOS TOKENS ANALISADOS ADICIONAR DENTRO DO TOKEN PROCEDURE
//linha 346 adicionar true e false

public class AnalisadorSintatico {

    private static List<Token> tokensAnalisados = new ArrayList<>();
    private static TokenSintatico programa = new TokenSintatico(PROGRAMA, new ArrayList<>());
    private static Stack<Token> guardaTokensObtidos = new Stack<>();
    private static TokenSintaticosEnum estadoAnalise;
    private static String procedureAnalisando = "";
    private static Token tokenAtual;
    private static String tokenComErro = "";

    public static Token start() {
        estadoAnalise = PROGRAMA;
        TokenSintatico bloco = null;
        try {
            tokenAtual = geraToken();
            bloco = regras();
        } catch (AnaliseException e) {
            System.out.println("\u001B[31m" + e.getTipo() + " Exception, " + e.getMessage() + "\u001B[0m");
        }

        tokensAnalisados.forEach(token -> token.imprimeErro());
        tokensAnalisados.forEach(token -> token.imprimeToken(""));

        return bloco;
    }

    private static TokenSintatico regras() throws AnaliseException {

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
                            if (!hasErroUltimoBloco(bloco))
                                tokenComErro = tokenAtual.getTipoToken().getDescricao() + " line: "
                                        + tokenAtual.getLinha();
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
        } catch (AnaliseException e) {
            throw e;
        } finally {
            programa.getTokens().add(programa.getTokens().size() - 1, bloco);
        }
        return bloco;
    }

    private static Boolean hasErroUltimoBloco(TokenSintatico estrutura) {
        Integer tamanho = estrutura.getTokens().size();
        if (tamanho > 0)
            return estrutura.getTokens().get(tamanho - 1).getTipoToken().equals(ERRO);
        return false;
    }

    private static Boolean analisaPrograma() throws AnaliseException {
        tokensAnalisados.add(programa);

        if (!esperaToken(tokenAtual, PROGRAM, programa)) {
            return trataErro(List.of(BEGIN, PROCEDURE, VAR));
        }
        if (!esperaToken(geraToken(), ID, programa)) {
            return trataErro(List.of(BEGIN, PROCEDURE, VAR));
        } else {
            TokenController.updateCategoriaTkIds(tokenAtual.getLexema(), "program");
        }
        if (!esperaToken(geraToken(), FDEFIN, programa)) {
            return trataErro(List.of(BEGIN, PROCEDURE, VAR));
        }
        estadoAnalise = BLOCO;
        geraToken();
        return true;
    }

    private static Boolean analisaDeclaracaoVariaveis(TokenSintatico tkPai) throws AnaliseException {
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

            if (tkPai.getTipoToken().equals(DECLARACAO_PROCEDIMENTO))
                listIds = AnalisadorSemantico.analisaDeclaracaoVariaveis(listIds, tokenAtual.getLexema(),
                        tkPai.getTokens().get(1).getLexema());
            else
                listIds = AnalisadorSemantico.analisaDeclaracaoVariaveis(listIds, tokenAtual.getLexema());

            if (!esperaToken(geraToken(), FDEFIN, declaracaoV))
                return trataErro(List.of(BEGIN, PROCEDURE, VAR));
        }
        addTkSintaticoFilho(tkPai, declaracaoV);
        estadoAnalise = BLOCO;
        return true;
    }

    private static Boolean analisaDeclaracaoProcedimento(TokenSintatico tkPai) throws AnaliseException {
        TokenSintatico declaracaoP = new TokenSintatico(DECLARACAO_PROCEDIMENTO, new ArrayList<>());

        if (!esperaToken(tokenAtual, PROCEDURE, declaracaoP))
            return trataErro(List.of(BEGIN, PROCEDURE, VAR));
        if (!esperaToken(geraToken(), ID, declaracaoP))
            return trataErro(List.of(BEGIN, PROCEDURE, VAR));
        String lexemaProcedure = tokenAtual.getLexema().toUpperCase();

        if (TokenController.getTkIds(lexemaProcedure).getCategoria() != null)
            throw new AnaliseException("Duplicate local variable " + lexemaProcedure, "Semantic");

        TokenController.updateCategoriaTkIds(lexemaProcedure, "procedure");
        var declaracao = TokenController.adicionaPalavraListaProcedures(lexemaProcedure, tokenAtual, null);

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
                List<Token> listIds = new ArrayList<>();
                listIds.add(tokenAtual);

                while (!(comparaToken(geraToken(), DECTIPO))) {
                    if (!esperaToken(tokenAtual, SEPLISTA, parametrosFormais))
                        return trataErro(List.of(BEGIN, PROCEDURE, VAR));
                    if (!esperaToken(geraToken(), ID, parametrosFormais))
                        return trataErro(List.of(BEGIN, PROCEDURE, VAR));
                    listIds.add(tokenAtual);
                }

                if (!esperaToken(tokenAtual, DECTIPO, parametrosFormais))
                    return trataErro(List.of(BEGIN, PROCEDURE, VAR));
                if (!esperaLstToken(geraToken(), List.of(INTEGER, REAL, BOOLEAN, STRING), parametrosFormais))
                    return trataErro(List.of(BEGIN, PROCEDURE, VAR));

                listIds.forEach(
                        id -> {
                            declaracao.getParametros().add(new TokenIdentificador(id, tokenAtual.getLexema()));
                        });

                if (!esperaLstToken(geraToken(), List.of(FDEFIN, RPAREN), parametrosFormais))
                    return trataErro(List.of(BEGIN, PROCEDURE, VAR));
            }

        }
        if (!esperaToken(geraToken(), FDEFIN, declaracaoP))
            return trataErro(List.of(BEGIN, PROCEDURE, VAR));

        if (comparaToken(geraToken(), VAR))
            analisaDeclaracaoVariaveis(declaracaoP);

        if (!esperaToken(tokenAtual, BEGIN, declaracaoP))
            return trataErro(List.of(BEGIN, PROCEDURE, VAR));

        procedureAnalisando = lexemaProcedure;
        analisaComandoComposto(List.of(END), declaracaoP);
        addTokenAnalisado(declaracaoP);

        if (!esperaToken(geraToken(), FDEFIN, declaracaoP)) {
            procedureAnalisando = "";
            return trataErro(List.of(BEGIN, PROCEDURE, VAR));
        }

        geraToken();
        addTkSintaticoFilho(tkPai, declaracaoP);
        procedureAnalisando = "";
        estadoAnalise = BLOCO;
        return true;
    }

    private static Boolean analisaComandoComposto(List<TipoToken> finalExpressao, TokenSintatico tkPai)
            throws AnaliseException {

        TokenSintatico comandoComposto = new TokenSintatico(COMANDO_COMPOSTO, new ArrayList<>());
        while (comparaLstToken(geraToken(), finalExpressao) == false) {
            analisaComando(finalExpressao, comandoComposto);
        }

        addTkSintaticoFilho(tkPai, comandoComposto);
        return true;
    }

    private static List<TipoToken> proceduresReservadas() {
        return List.of(WRITE, WRITELN, READ, READLN, PRINT);
    }

    private static Boolean analisaComando(List<TipoToken> finalExpressao, TokenSintatico tkPai)
            throws AnaliseException {
        TokenSintatico comando = new TokenSintatico(COMANDO, new ArrayList<>());
        List<TipoToken> lstTksProced = proceduresReservadas();

        if (comparaToken(tokenAtual, ID) || comparaLstToken(tokenAtual, lstTksProced)) {
            var tokenID = tokenAtual;
            if (comparaLstToken(geraToken(), List.of(FDEFIN, LPAREN))) {
                List<TipoToken> fimExpressao = finalExpressao;
                if (tkPai.getTipoToken().equals(COMANDO_COMPOSTO))
                    fimExpressao = List.of(FDEFIN);
                comando.getTokens().add(tokenID);
                analisaChamadaProcedimento(comando, fimExpressao);
            } else {
                TokenSintatico estruturaAt = new TokenSintatico(ESTRUTURA_ATRIBUICAO, new ArrayList<>());
                if (!esperaLstToken(tokenAtual, List.of(ATRIBUICAO), estruturaAt)) {
                    if (comando.getTokens().size() - 2 >= 0)
                        comando.getTokens().remove(comando.getTokens().size() - 2);
                    trataErro(List.of(FDEFIN));
                } else {
                    estruturaAt.getTokens().add(0, tokenID);
                    List<TipoToken> fimExpressao = finalExpressao;
                    if (tkPai.getTipoToken().equals(COMANDO_COMPOSTO))
                        fimExpressao = List.of(FDEFIN);
                    analisaExpressaoTrecho(fimExpressao, estruturaAt);
                    removerUltimoTokenLstExpressao(estruturaAt, List.of(FDEFIN, ELSE));
                    estruturaAt.getTokens().add(tokenAtual);
                    addTkSintaticoFilho(comando, estruturaAt);
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
        addTkSintaticoFilho(tkPai, comando);
        return true;
    }

    private static void addTkSintaticoFilho(TokenSintatico tkPai, TokenSintatico tkFilho) throws AnaliseException {

        List<TipoToken> lista = List.of(ESTRUTURA_ATRIBUICAO, LISTA_EXPRESSAO,
                REPETITIVO, CHAMADA_PROCEDIMENTO,
                CONDICIONAL);
        if (comparaLstToken(tkFilho, lista)) {
            Map<String, TokenIdentificador> listaIds;
            if (procedureAnalisando.equals("")) {
                listaIds = TokenController.getListaIdentificadores();
            } else {
                TokenProcedure procedure = TokenController.getListaProcedures().get(procedureAnalisando);
                listaIds = procedure == null ? new HashMap<>() : procedure.getListaIdentificadores();
            }
            tkFilho = AnalisadorSemantico.analisaSemantica(tkFilho, listaIds);
        }
        tkPai.getTokens().add(tkFilho);
    }

    private static Boolean analisaChamadaProcedimento(TokenSintatico tkPai, List<TipoToken> finalExpressao)
            throws AnaliseException {
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

            }
            geraToken();
        }
        if (!esperaLstToken(tokenAtual, finalExpressao, chamadaProcedimento))
            return trataErro(List.of(FDEFIN));
        addTkSintaticoFilho(tkPai, chamadaProcedimento);
        return true;
    }

    private static Boolean analisaExpressaoTrecho(List<TipoToken> fExpressoes, TokenSintatico tkPai)
            throws AnaliseException {

        TokenSintatico lstExp = new TokenSintatico(LISTA_EXPRESSAO, new ArrayList<>());
        List<TipoToken> listaCmp = List.of(ID, VALORINT, VALORLIT, VALOREAL, OPARITSOMA, OPARITSUB, OPALOGNOT, LPAREN);

        if (!esperaLstToken(geraToken(), listaCmp, lstExp))
            return trataErro(fExpressoes);

        Integer qtd = 0;
        while (!comparaLstToken(tokenAtual, fExpressoes)) {
            analisaExpressao(fExpressoes, lstExp, qtd++);
        }
        for (Token t : lstExp.getTokens()) {
            if (t.getTipoToken().equals(ERRO)) {
                return trataErro(fExpressoes);
            }
        }
        addTkSintaticoFilho(tkPai, lstExp);
        return true;
    }

    private static List<TipoToken> concArray(List<TipoToken> array1, List<TipoToken> array2) {
        List<TipoToken> retorno = array1;
        retorno.addAll(array2);
        return retorno;
    }

    private static Boolean analisaExpressao(List<TipoToken> finalExpressoes, TokenSintatico tkPai, Integer qtd)
            throws AnaliseException {

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
        if (ultimaExpressaoAnalisada.getTipoToken().equals(LISTA_EXPRESSAO)) {
            List<Token> lstTokensExpressao = ((TokenSintatico) ultimaExpressaoAnalisada).getTokens();
            if (lstTokensExpressao.get(lstTokensExpressao.size() - 1).getTipoToken().equals(TIPO))
                lstTokensExpressao.remove(lstTokensExpressao.size() - 1);
        }
    }

    private static void removerUltimoTokenLstExpressao(TokenSintatico retirarDe, List<TipoToken> TIPO) {
        for (TipoToken tipoToken : TIPO) {
            removerUltimoTokenLstExpressao(retirarDe, tipoToken);
        }
    }

    private static Boolean analisaRepetitivo(TokenSintatico tkPai) throws AnaliseException {
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
            TokenSintatico estruturaAt = new TokenSintatico(ESTRUTURA_ATRIBUICAO, new ArrayList<>());
            Token esperaId = geraToken();
            Token esperaAtribuicao = geraToken();
            if (!esperaToken(esperaId, ID, estruturaAt))
                return trataErro(List.of(FDEFIN));
            if (!esperaToken(esperaAtribuicao, ATRIBUICAO, estruturaAt))
                return trataErro(List.of(FDEFIN));
            analisaExpressaoTrecho(List.of(TO, DOWNTO), estruturaAt);
            removerUltimoTokenLstExpressao(estruturaAt, List.of(TO, DOWNTO));
            addTkSintaticoFilho(tkRepetitivo, estruturaAt);

            removerUltimoTokenLstExpressao(tkRepetitivo, tokenAtual.getTipoToken());
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
        addTkSintaticoFilho(tkPai, tkRepetitivo);
        return true;
    }

    private static Boolean analisaCondicional(TokenSintatico tkPai, List<TipoToken> finalExpressao)
            throws AnaliseException {

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

        addTkSintaticoFilho(tkPai, tkCondicional);
        return true;
    }

    private static Boolean trataErro(List<TipoToken> proximosTokenValido) throws AnaliseException {
        tokenComErro = tokenAtual.getTipoToken().getDescricao() + " line: " + tokenAtual.getLinha();
        // System.out.println("\u001B[31m ----------- ERRO ------------ \u001B[0m");
        while (geraToken() != null) {
            if (comparaLstToken(tokenAtual, proximosTokenValido)) {
                return false;
            } else if (comparaToken(tokenAtual, FIMLEITURA)) {
                return false;
            }
        }
        throw new AnaliseException("Was not possible to recover the program from token " + tokenComErro, "Syntax");
    }

    private static boolean comparaToken(Token tokenRecebido, TipoToken tokenEsperado) {
        return tokenRecebido.getTipoToken().equals(tokenEsperado);
    }

    private static boolean comparaLstToken(Token tokenRecebido, List<TipoToken> tokensEsperados) {

        for (TipoToken tipoToken : tokensEsperados) {
            if (tokenRecebido.getTipoToken().equals(tipoToken)) {
                return true;
            }
        }
        return false;
    }

    private static Boolean esperaToken(Token tokenRecebido, TipoToken tokenEsperado, TokenSintatico tkPai)
            throws AnaliseException {

        String mensagem = "expecting '" + tokenEsperado.getDescricao() + "' ";
        mensagem += "but '" + tokenRecebido.getTipoToken().getDescricao() + "' found";

        if (tokenRecebido.getTipoToken().equals(ERRO)) {
            mensagem = tokenRecebido.getLexema();
            tkPai.getTokens().add(tokenRecebido);
            return false;
        }

        Boolean isCorreto = tokenRecebido.getTipoToken().equals(tokenEsperado);
        Token esperaToken = isCorreto ? tokenRecebido : new Token(ERRO, mensagem);
        tkPai.getTokens().add(esperaToken);

        return isCorreto;
    }

    private static Boolean esperaLstToken(Token tokenRecebido, List<TipoToken> tokensEsperados, TokenSintatico tkPai) {
        String mensagem = "not expecting '" + tokenRecebido.getTipoToken().getDescricao() + "'";

        if (tokenRecebido.getTipoToken().equals(ERRO)) {
            mensagem = tokenRecebido.getLexema();
            tkPai.getTokens().add(tokenRecebido);
            return false;
        }

        for (TipoToken tipoToken : tokensEsperados) {
            if (tokenRecebido.getTipoToken().equals(tipoToken)) {
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

    private static Token geraToken() throws AnaliseException {
        guardaTokensObtidos.push(tokenAtual);
        tokenAtual = AnalisadorLexico.proximoToken();
        while (tokenAtual == null
                || comparaLstToken(tokenAtual, List.of(COMENTLIN, LCOMENTK, LCOMENTP, RCOMENTK, RCOMENTP))) {
            tokenAtual = AnalisadorLexico.proximoToken();
        }

        if (tokenAtual.getTipoToken().equals(FIMLEITURA)) {
            String mensagem = "Was not possible to recover the program from token " + tokenComErro
                    + ", token chain ended before.";
            throw new AnaliseException(mensagem, "Syntax");
        }

        // System.out.println("-------------------------------------------");
        // System.out.println(tokenAtual);

        return tokenAtual;
    }

    private static Token voltaToken() {
        AnalisadorLexico.pushToken(tokenAtual);
        tokenAtual = guardaTokensObtidos.pop();

        // System.out.println("----------------VOLTA----------------------");
        // System.out.println(tokenAtual);

        return tokenAtual;
    }
}
