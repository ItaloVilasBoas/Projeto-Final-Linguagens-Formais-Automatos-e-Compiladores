import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;

import Analisador.AnalisadorSintatico;
import Analisador.TokenController;
import Exceptions.AnaliseException;
import Token.Token;
import Token.TokenIdentificador;
import Token.TokenProcedure;
import Token.TokenSemantico;
import Token.TokenSintatico;
import Token.Enums.TipoToken;
import Token.Enums.TokenSintaticosEnum;
import Token.Enums.TokensReservados;

import static Token.Enums.TokenSintaticosEnum.*;
import static Token.Enums.TokensReservados.*;

public class Compilador {

    private static String input;
    private static Boolean inputAcces;
    private static JTextPane output;
    private static SimpleAttributeSet outputColor;
    private static SimpleAttributeSet error;
    private Map<String, TokenIdentificador> listaIdentificadores;

    public Compilador(Map<String, TokenIdentificador> listaIdentificadores, JTextPane output,
            SimpleAttributeSet outputColor, SimpleAttributeSet error) {

        this.listaIdentificadores = listaIdentificadores;
        Compilador.output = output;
        Compilador.outputColor = outputColor;
        Compilador.error = error;
        input = "";
        inputAcces = false;
    }

    public Compilador(Map<String, TokenIdentificador> listaIdentificadores) {
        this.listaIdentificadores = listaIdentificadores;
    }

    public void executaPrograma() throws AnaliseException {
        Token bloco = null;
        try {
            bloco = AnalisadorSintatico.start();
        } catch (Exception e) {
            escreveOutput(error, List.of(e.getMessage()), true);
        } finally {
            AnalisadorSintatico.getTokensAnalisados().forEach(token -> {
                escreveOutput(error, token.imprimeErro(), true);
            });
        }
        try {
            if (bloco != null)
                executaBloco(bloco);
        } catch (Exception e) {
            throw e;
        } finally {
            escreveOutput(outputColor, List.of("\nPrograma finalizado."), true);
        }

    }

    public void executaBloco(Token bloco) throws AnaliseException {
        List<Token> comandos = new ArrayList<>();

        if (bloco instanceof TokenSintatico)
            for (Token token : ((TokenSintatico) bloco).getTokens()) {
                if (token.getTipoToken().equals(COMANDO_COMPOSTO))
                    comandos = ((TokenSintatico) token).getTokens();
            }

        for (Token comando : comandos) {
            if ((comando instanceof TokenSintatico) && (comando.getTipoToken().equals(COMANDO)))
                executaComando((TokenSintatico) comando);
        }

    }

    private void executaComando(TokenSintatico comando) throws AnaliseException {

        for (Token token : comando.getTokens()) {
            TokenSintaticosEnum tipoToken = null;
            if (token.getTipoToken() instanceof TokenSintaticosEnum) {
                tipoToken = (TokenSintaticosEnum) token.getTipoToken();
                switch (tipoToken) {
                    case ESTRUTURA_ATRIBUICAO:
                        executaAtribuicao((TokenSemantico) token);
                        break;
                    case LISTA_EXPRESSAO:
                        calculaListaExpressao((TokenSemantico) token);
                        break;
                    case CHAMADA_PROCEDIMENTO:
                        executaProcedimento((TokenSemantico) token);
                        break;
                    case CONDICIONAL:
                        executaCondicional((TokenSemantico) token);
                        break;
                    case REPETITIVO:
                        executaRepetitivo((TokenSemantico) token);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void escreveOutput(SimpleAttributeSet attributeSet, List<String> parametros, Boolean printLn) {
        try {
            for (String parametro : parametros) {
                if (parametro.length() > 0 && parametro.charAt(0) == '\'')
                    parametro = parametro.substring(1, parametro.length() - 1);
                output.getStyledDocument().insertString(output.getStyledDocument().getLength(), parametro,
                        attributeSet);
            }
            if (printLn)
                output.getStyledDocument().insertString(output.getStyledDocument().getLength(), "\n",
                        attributeSet);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private synchronized String waitInput(Boolean printLn) {
        inputAcces = true;
        while (input.equals("")) {
            try {
                while (input.equals(""))
                    wait(1000);
                if (printLn)
                    escreveOutput(outputColor, List.of(""), true);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        inputAcces = false;
        String retornaInput = input;
        setInput("");
        return retornaInput;
    }

    private void executaProcedimento(TokenSemantico tokenProcedure) throws AnaliseException {

        Token idProcedure = tokenProcedure.getTokens().get(0);
        if (idProcedure.getTipoToken().equals(WRITE) || idProcedure.getTipoToken().equals(WRITELN)) {
            List<String> parametros = new ArrayList<>();
            for (Token token : tokenProcedure.getTokens()) {
                if (token.getTipoToken().equals(LISTA_EXPRESSAO)) {
                    parametros.add(calculaListaExpressao((TokenSemantico) token));
                }
            }

            if (idProcedure.getTipoToken().equals(WRITE)) {
                escreveOutput(outputColor, parametros, false);
            } else {
                escreveOutput(outputColor, parametros, true);
            }

        } else if (idProcedure.getTipoToken().equals(READ) || idProcedure.getTipoToken().equals(READLN)) {
            List<String> parametros = new ArrayList<>();
            for (Token token : tokenProcedure.getTokens()) {
                if (token.getTipoToken().equals(LISTA_EXPRESSAO)) {
                    var fatorId = listaIdentificadores
                            .get(((TokenSemantico) token).getValor().toUpperCase().trim());
                    if (fatorId == null) {
                        throw new AnaliseException("Error: Variable identifier expected", "Compiler");
                    } else if (fatorId.getTipoDado().equals(BOOLEAN.getDescricao())) {
                        throw new AnaliseException("Error: can't read variable of type BOOLEAN", "Compiler");
                    } else {
                        parametros.add(((TokenSemantico) token).getValor().toUpperCase().trim());
                    }
                }
            }
            for (String parametro : parametros) {
                TokenIdentificador id = listaIdentificadores.get(parametro);
                String retorno = waitInput(true);
                String[] buffer = splitExpressao(retorno);
                id.setValor(buffer[0]);
                setInput(String.join(" ", Arrays.asList(buffer).subList(1, buffer.length)));

                if (id.getTipoDado().equals(INTEGER.getDescricao())) {
                    try {
                        Integer.parseInt(id.getValor().trim());
                    } catch (Exception e) {
                        throw new AnaliseException("Error: cannot convert this input to INTEGER", "Compiler");
                    }
                } else if (id.getTipoDado().equals(REAL.getDescricao())) {
                    try {
                        Double.parseDouble(id.getValor().trim());
                    } catch (Exception e) {
                        throw new AnaliseException("Error: cannot convert this input to REAL", "Compiler");
                    }
                }
            }
            if (idProcedure.getTipoToken().equals(READLN))
                setInput("");

        } else {
            chamaProcedimento(tokenProcedure);
        }
    }

    private void chamaProcedimento(TokenSemantico tokenProcedure) throws AnaliseException {

        Token idProcedure = tokenProcedure.getTokens().get(0);
        List<String> parametrosValor = new ArrayList<>();
        List<String> parametrosRef = new ArrayList<>();
        TokenProcedure procedure = TokenController.getListaProcedures().get(idProcedure.getLexema().toUpperCase());
        Map<String, TokenIdentificador> listaIdsExecucao = new HashMap<>(procedure.getListaIdentificadores());

        Integer index = 0;
        for (Token token : tokenProcedure.getTokens()) {
            if (token.getTipoToken().equals(LISTA_EXPRESSAO)) {
                String lexema = procedure.getParametros().get(index).getLexema();
                String valorExpressao = calculaListaExpressao((TokenSemantico) token);

                if ((procedure.getParametros().get(index).getCategoria().equals("PARAMETRO VAL"))
                        && (TokenController.getListaIdentificadores().get(lexema) != null))
                    parametrosValor.add(lexema);
                else
                    parametrosRef.add(lexema);

                listaIdsExecucao.get(lexema).setValor(valorExpressao);
                index++;
            }
        }

        new Compilador(listaIdsExecucao).executaBloco(procedure.getComandos());
        listaIdsExecucao.forEach((chave, token) -> {
            if (token.getCategoria().equals("PARAMETRO VAL"))
                TokenController.getListaIdentificadores().get(chave).setValor(token.getValor());
        });

    }

    private void executaRepetitivo(TokenSemantico repetitivo) throws AnaliseException {
        Token tipoRepetitivo = repetitivo.getTokens().get(0);
        List<Token> comandos = encontraComandos(repetitivo);
        String expressao = repetitivo.getValor();

        if (tipoRepetitivo.getTipoToken().equals(WHILE)) {
            while (calculaListaExpressao(expressao).equals("true")) {
                for (Token comando : comandos) {
                    executaComando((TokenSintatico) comando);
                }
            }
        } else if (tipoRepetitivo.getTipoToken().equals(REPEAT)) {
            do {
                for (Token comando : comandos) {
                    executaComando((TokenSintatico) comando);
                }
            } while (calculaListaExpressao(expressao).equals("false"));
        } else {
            Token toDownTo = repetitivo.getTokens().get(2);
            TokenSemantico atribuicao = (TokenSemantico) repetitivo.getTokens().get(1);
            TokenIdentificador id = listaIdentificadores.get(atribuicao.getTokens().get(0).getLexema().toUpperCase());
            executaAtribuicao(atribuicao);

            Integer valorId = Integer.parseInt(id.getValor());
            Integer operador = toDownTo.getTipoToken().equals(TO) ? 1 : -1;
            Integer valorExpressao = Integer.parseInt(calculaListaExpressao(expressao));

            for (; valorId <= valorExpressao; valorId += operador) {
                id.setValor(valorId.toString());
                for (Token comando : comandos) {
                    executaComando((TokenSintatico) comando);
                }
                valorExpressao = Integer.parseInt(calculaListaExpressao(expressao));
            }
        }
    }

    private List<Token> encontraComandos(TokenSemantico tokenComComando) {
        List<Token> comandos = new ArrayList<>();
        for (Token comando : tokenComComando.getTokens()) {
            if (comando.getTipoToken().equals(COMANDO)) {
                comandos.add(comando);
            } else if (comando.getTipoToken().equals(COMANDO_COMPOSTO)) {
                comandos.addAll(encontraComandos(new TokenSemantico((TokenSintatico) comando, "", "")));
            }
        }
        return comandos;
    }

    private void executaCondicional(TokenSemantico condicional) throws AnaliseException {
        String verificaCondicional = calculaListaExpressao(condicional.getValor());

        List<Token> comandos = encontraComandosCondicional(condicional);

        if (verificaCondicional.equals("true")) {

            List<Token> comandosExecutaveis = new ArrayList<>();
            if (comandos.get(0).getTipoToken().equals(COMANDO_COMPOSTO)) {

                for (Token comando : ((TokenSintatico) comandos.get(0)).getTokens()) {
                    if (comando.getTipoToken().equals(COMANDO)) {
                        comandosExecutaveis.add(comando);
                    }
                }
            } else {
                comandosExecutaveis.add(comandos.get(0));
            }

            for (Token comando : comandosExecutaveis) {
                executaComando((TokenSintatico) comando);
            }
        } else if (comandos.size() > 1) {

            List<Token> comandosExecutaveis = new ArrayList<>();
            if (comandos.get(1).getTipoToken().equals(COMANDO_COMPOSTO)) {

                for (Token comando : ((TokenSintatico) comandos.get(1)).getTokens()) {
                    if (comando.getTipoToken().equals(COMANDO)) {
                        comandosExecutaveis.add(comando);
                    }
                }
            } else {
                comandosExecutaveis.add(comandos.get(1));
            }

            for (Token comando : comandosExecutaveis) {
                executaComando((TokenSintatico) comando);
            }
        }

    }

    private List<Token> encontraComandosCondicional(TokenSemantico tokenComComando) {
        List<Token> comandos = new ArrayList<>();
        for (Token comando : tokenComComando.getTokens()) {
            if (comando.getTipoToken().equals(COMANDO) || comando.getTipoToken().equals(COMANDO_COMPOSTO)) {
                comandos.add(comando);
            }
        }
        return comandos;
    }

    private void executaAtribuicao(TokenSemantico tokenAtribuicao) throws AnaliseException {
        List<Token> atribuicao = tokenAtribuicao.getTokens();
        var id = atribuicao.get(0);
        var listaExpressao = (TokenSemantico) atribuicao.get(2);

        var idLista = listaIdentificadores.get(id.getLexema().toUpperCase());
        idLista.setValor(calculaListaExpressao(listaExpressao));
    }

    private String calculaListaExpressao(TokenSemantico tokenListaExpressao) throws AnaliseException {

        return calculaListaExpressao(tokenListaExpressao.getValor());
    }

    private String calculaListaExpressao(String listaExpressao) throws AnaliseException {

        String[] expressoes = splitExpressao(listaExpressao);

        for (Integer i = 0; i < expressoes.length; i++) {
            String expressao = expressoes[i];
            TokenIdentificador fatorId = listaIdentificadores.get(expressao.toUpperCase());
            if (fatorId != null) {
                if (fatorId.getValor() == null)
                    throw new AnaliseException("The local variable " + expressao + " not have been initialized",
                            "Compiler");
                if (fatorId.getTipoDado().equals(TokensReservados.STRING.descricao))
                    expressoes[i] = "'" + fatorId.getValor() + "'";
                else
                    expressoes[i] = fatorId.getValor();
            }
        }

        try {
            return eval(new ArrayList<>(Arrays.asList(expressoes))).toString();
        } catch (Exception e) {
            throw new AnaliseException("Was not possible to resolve the expression " + listaExpressao, "Compiler");
        }
    }

    private String[] splitExpressao(String expressao) {

        List<String> retorno = new ArrayList<String>();
        String auxiliar = "";
        Boolean quotes = false;
        for (Character word : expressao.toCharArray()) {
            if (word == '\'')
                quotes = !quotes;
            if (word == ' ' && !quotes) {
                retorno.add(auxiliar);
                auxiliar = "";
            } else
                auxiliar += word.toString();
        }
        if (!auxiliar.isBlank())
            retorno.add(auxiliar);
        return retorno.toArray(new String[(retorno.size())]);
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

    private static String eval(List<String> expressoes) throws AnaliseException {

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
            int prec = p2 != -1 ? p2 : (p3 != -1 ? p3 : rel);

            if (lparen != -1) {
                int rparen = posicaoRparen(expressoes.subList(lparen + 1, expressoes.size()));
                var copiaTrecho = new ArrayList<>(expressoes.subList(lparen + 1, lparen + rparen + 1));
                expressoes.subList(lparen, lparen + rparen + 2).clear();
                var resultado = eval(copiaTrecho);
                expressoes.add(lparen, resultado);
            } else if (not != -1) {
                expressoes.set(not + 1,
                        (!Boolean.parseBoolean(expressoes.get(not + 1))) + "");
                expressoes.remove(not);
            } else if (andOr != -1) {
                Boolean bolf;
                var bol1 = Boolean.parseBoolean(expressoes.get(andOr - 1));
                var bol2 = Boolean.parseBoolean(expressoes.get(andOr + 1));
                if (expressoes.get(andOr).equals(OPALOGAND.getDescricao()))
                    bolf = (bol1 && bol2);
                else
                    bolf = (bol1 || bol2);
                expressoes.set(andOr - 1, bolf.toString());
                expressoes.remove(andOr);
                expressoes.remove(andOr);
            } else if (prec != -1) {
                String resultado;
                if (p2 != -1 || p3 != -1)
                    resultado = operacaoMatematica(expressoes.get(prec - 1), expressoes.get(prec + 1),
                            expressoes.get(prec));
                else
                    resultado = operacaoLogica(expressoes.get(prec - 1), expressoes.get(prec + 1),
                            expressoes.get(prec)).toString();
                expressoes.set(prec - 1, resultado);
                expressoes.remove(prec);
                expressoes.remove(prec);
            }
        }

        return expressoes.get(0);
    }

    private static Boolean operacaoLogica(String string1, String string2, String operador) {
        Double expressao1 = null;
        Double expressao2 = null;

        try {
            expressao1 = Double.parseDouble(string1);
        } catch (Exception e) {
        }
        try {
            expressao2 = Double.parseDouble(string2);
        } catch (Exception e) {
        }

        if (operador.equals(OPRELEQUAL.getDescricao()) && (expressao1 == null && expressao2 == null))
            return string1.equals(string2);
        if (operador.equals(OPRELNEQUAL.getDescricao()) && (expressao1 == null && expressao2 == null))
            return !string1.equals(string2);

        if (operador.equals(OPRELGREAT.getDescricao()))
            return (Double) expressao1 > (Double) expressao2;
        if (operador.equals(OPRELLESS.getDescricao()))
            return (Double) expressao1 < (Double) expressao2;
        if (operador.equals(OPRELGEQUAL.getDescricao()))
            return (Double) expressao1 >= (Double) expressao2;
        if (operador.equals(OPRELLEQUAL.getDescricao()))
            return (Double) expressao1 <= (Double) expressao2;
        if (operador.equals(OPRELEQUAL.getDescricao()))
            return ((Double) expressao1).compareTo((Double) expressao2) == 0;
        if (operador.equals(OPRELNEQUAL.getDescricao()))
            return ((Double) expressao1).compareTo((Double) expressao2) != 0;

        return false;
    }

    private static String operacaoMatematica(String strNum1, String strNum2, String operador) {
        Number num1 = Double.parseDouble(strNum1);
        Number num2 = Double.parseDouble(strNum2);
        String resultado = "";

        if (operador.equals(OPARITMULT.getDescricao()))
            resultado = ((Double) num1 * (Double) num2) + "";
        if (operador.equals(OPARITDIV.getDescricao()))
            resultado = ((Double) num1 / (Double) num2) + "";
        if (operador.equals(OPARITDIVINT.getDescricao()))
            resultado = ((Integer) num1 / (Integer) num2) + "";
        if (operador.equals(OPARITMOD.getDescricao()))
            resultado = ((Double) num1 % (Double) num2) + "";
        if (operador.equals(OPARITSOMA.getDescricao()))
            resultado = ((Double) num1 + (Double) num2) + "";
        if (operador.equals(OPARITSUB.getDescricao()))
            resultado = ((Double) num1 - (Double) num2) + "";

        if (Double.parseDouble(resultado) % 1 == 0) {
            resultado = resultado.split("\\.")[0];
        }

        return resultado;
    }

    public Map<String, TokenIdentificador> getListaIdentificadores() {
        return listaIdentificadores;
    }

    public void setListaIdentificadores(Map<String, TokenIdentificador> listaIdentificadores) {
        this.listaIdentificadores = listaIdentificadores;
    }

    public static String getInput() {
        return input;
    }

    public static void setInput(String input) {
        Compilador.input = input;
    }

    public static Boolean getInputAcces() {
        return inputAcces;
    }

    public static void setInputAcces(Boolean inputAcces) {
        Compilador.inputAcces = inputAcces;
    }
}
