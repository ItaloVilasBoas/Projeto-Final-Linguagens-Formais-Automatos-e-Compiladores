import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
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
import Token.Enums.TokenSintaticosEnum;
import Token.Enums.TokensReservados;

import static Token.Enums.TokenSintaticosEnum.*;
import static Token.Enums.TokensReservados.*;

public class Compilador {

    private static String input = "";
    private static Boolean inputAcces = false;
    private static JTextPane output;
    private static SimpleAttributeSet outputColor;
    private static SimpleAttributeSet error;
    private Map<String, TokenIdentificador> listaIdentificadores;
    private ScriptEngine engine = new ScriptEngineManager().getEngineByName("javascript");

    public Compilador(Map<String, TokenIdentificador> listaIdentificadores, JTextPane output,
            SimpleAttributeSet outputColor, SimpleAttributeSet error) {

        this.listaIdentificadores = listaIdentificadores;
        Compilador.output = output;
        Compilador.outputColor = outputColor;
        Compilador.error = error;
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
        if (bloco != null)
            executaBloco(bloco);

        escreveOutput(outputColor, List.of("\nPrograma finalizado."), true);
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
                Boolean prinln = idProcedure.getTipoToken().equals(READLN);
                id.setValor(waitInput(prinln));
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
                executaComando((TokenSintatico) comandos.get(0));
            }
        } else if (tipoRepetitivo.getTipoToken().equals(REPEAT)) {
            do {
                executaComando((TokenSintatico) comandos.get(0));
            } while (calculaListaExpressao(expressao).equals("true"));
        } else {
            Token toDownTo = repetitivo.getTokens().get(2);
            TokenSemantico atribuicao = (TokenSemantico) repetitivo.getTokens().get(1);
            TokenIdentificador id = listaIdentificadores.get(atribuicao.getTokens().get(0).getLexema().toUpperCase());
            executaAtribuicao(atribuicao);

            Integer valorId = Integer.parseInt(id.getValor());
            Integer operador = toDownTo.getTipoToken().equals(TO) ? 1 : -1;
            Integer valorExpressao = Integer.parseInt(calculaListaExpressao(expressao));

            for (; valorId != valorExpressao; valorId += operador) {
                executaComando((TokenSintatico) comandos.get(0));
                valorExpressao = Integer.parseInt(calculaListaExpressao(expressao));
                id.setValor(valorId.toString());
            }
        }
    }

    private List<Token> encontraComandos(TokenSemantico tokenComComando) {
        List<Token> comandos = new ArrayList<>();
        for (Token comando : tokenComComando.getTokens()) {
            if (comando.getTipoToken().equals(COMANDO) || comando.getTipoToken().equals(COMANDO_COMPOSTO)) {
                comandos.add(comando);
            }
        }
        return comandos;
    }

    private void executaCondicional(TokenSemantico condicional) throws AnaliseException {
        condicional.setValor(calculaListaExpressao(condicional.getValor()));

        List<Token> comandos = encontraComandos(condicional);

        if (condicional.getValor().equals("true")) {
            executaComando((TokenSintatico) comandos.get(0));
        } else if (comandos.size() > 1) {
            executaComando((TokenSintatico) comandos.get(1));
        }

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

        String[] expressoes = listaExpressao.split("\\s(?![^']*')");

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
            } else if (expressao.equals("AND")) {
                expressoes[i] = "&&";
            } else if (expressao.equals("OR")) {
                expressoes[i] = "||";
            } else if (expressao.equals("=")) {
                expressoes[i] = "==";
            } else if (expressao.equals("TRUE")) {
                expressoes[i] = "true";
            } else if (expressao.equals("FALSE")) {
                expressoes[i] = "false";
            }

        }

        String expressaoFinal = String.join(" ", expressoes);
        try {
            return engine.eval(expressaoFinal).toString();
        } catch (ScriptException e) {
            throw new AnaliseException("Was not possible to resolve the expression " + listaExpressao, "Compiler");
        }
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
