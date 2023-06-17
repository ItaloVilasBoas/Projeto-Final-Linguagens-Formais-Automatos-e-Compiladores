package Analisador;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import Token.Token;
import Token.Enums.EstadosEnum;
import Token.Enums.TokenGenerico;
import Token.Enums.TokensReservados;

public class AnalisadorLexico {

    private static Deque<Token> tksEncontrados = new LinkedList<>();
    private static BufferedReader reader;
    private static String line;

    public AnalisadorLexico(String fileName) {
        try {
            AnalisadorLexico.reader = new BufferedReader(new FileReader(fileName));
        } catch (Exception e) {
            System.out.println("Nao foi possivel realizar a leitura do arquivo.");
        }
    }

    public static void pushToken(Token token) {
        tksEncontrados.addFirst(token);
    }

    public static Token proximoToken() {
        if (tksEncontrados.isEmpty()) {
            geraProximosTokens();
        }
        return tksEncontrados.isEmpty() ? null : tksEncontrados.poll();
    }

    private static void geraProximosTokens() {
        Boolean comentarioK = false;
        Boolean comentarioP = false;
        try {
            do {
                line = reader.readLine();
                if (line != null) {
                    for (Token token : geraToken(line)) {
                        if (!comentarioK && !comentarioP)
                            tksEncontrados.add(token);
                        if (token.getTipo().equals(TokensReservados.LCOMENTK) && (!comentarioK && !comentarioP)) {
                            comentarioK = true;
                        } else if (comentarioK && token.getTipo().equals(TokensReservados.RCOMENTK)) {
                            comentarioK = false;
                            tksEncontrados.add(token);
                        } else if (token.getTipo().equals(TokensReservados.LCOMENTP)
                                && (!comentarioK && !comentarioP)) {
                            comentarioP = true;
                        } else if (comentarioP && token.getTipo().equals(TokensReservados.RCOMENTP)) {
                            comentarioP = false;
                            tksEncontrados.add(token);
                        } else if (token.getTipo().equals(TokensReservados.COMENTLIN)
                                && (!comentarioK && !comentarioP)) {
                            break;
                        }
                    }
                } else {
                    reader.close();
                    tksEncontrados.add(new Token(TokenGenerico.FIMLEITURA, line));
                }
            } while ((comentarioK || comentarioP));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Token> geraToken(String palavra) {
        EstadosEnum estado = EstadosEnum.INICIO_LEITURA;
        List<Token> tksLidosOutroAnalisador = new ArrayList<>();
        List<Token> tksEncontrados = new ArrayList<>();
        Integer indice = 0;

        if (palavra.trim().equals(""))
            return tksEncontrados;

        analisador: for (Character caracter : palavra.toCharArray()) {
            switch (estado) {
                case INICIO_LEITURA:
                    if (Character.isLetter(caracter))
                        estado = EstadosEnum.LIDO_IDENTIFICADOR;
                    else if (Character.isDigit(caracter))
                        estado = EstadosEnum.LIDO_NUMERO_INTEIRO;
                    else if (caracter == '\'')
                        estado = EstadosEnum.LIDO_LITERAL_INCOMPLETO;
                    else
                        estado = EstadosEnum.LIDO_CARACTER_ESPECIAL;
                    break;
                case LIDO_CARACTER_ESPECIAL:
                    String operador = palavra.substring(indice - 1, indice + 1);
                    if (operador.equals(">=") || operador.equals("<=") || operador.equals("<>") ||
                            operador.equals("(*") || operador.equals("*)") || operador.equals(":=") ||
                            operador.equals("//")) {
                        tksLidosOutroAnalisador.addAll(geraToken(palavra.substring(indice + 1)));
                        palavra = palavra.substring(0, indice + 1);
                        break analisador;
                    } else {
                        tksLidosOutroAnalisador.addAll(geraToken(palavra.substring(indice)));
                        palavra = palavra.substring(0, indice);
                        break analisador;
                    }
                case LIDO_IDENTIFICADOR:
                    if (caracter == '_' || Character.isLetter(caracter) || Character.isDigit(caracter))
                        estado = EstadosEnum.LIDO_IDENTIFICADOR;
                    else {
                        tksLidosOutroAnalisador.addAll(geraToken(palavra.substring(indice)));
                        palavra = palavra.substring(0, indice);
                        break analisador;
                    }
                    break;
                case LIDO_NUMERO_INTEIRO:
                    if (Character.isDigit(caracter))
                        estado = EstadosEnum.LIDO_NUMERO_INTEIRO;
                    else if (caracter == '.')
                        estado = EstadosEnum.LIDO_NUMERO_REAL_INCOMPLETO;
                    else if (Character.isLetter(caracter)) {
                        estado = EstadosEnum.LIDO_CARACTER_INVALIDO;
                        Integer prox = palavra.indexOf(" ");
                        Integer pontoVirgula = palavra.contains(";") ? palavra.indexOf(";") : palavra.indexOf(" ");
                        prox = (prox == -1 || pontoVirgula < prox) ? pontoVirgula : prox;
                        if (prox != -1) {
                            tksLidosOutroAnalisador.addAll(geraToken(palavra.substring(prox)));
                            palavra = palavra.substring(0, prox);
                        }
                        break analisador;
                    } else {
                        tksLidosOutroAnalisador.addAll(geraToken(palavra.substring(indice)));
                        palavra = palavra.substring(0, indice);
                        break analisador;
                    }
                    break;
                case LIDO_LITERAL_INCOMPLETO:
                    if (caracter == '\'')
                        estado = EstadosEnum.LIDO_LITERAL;
                    else
                        estado = EstadosEnum.LIDO_LITERAL_INCOMPLETO;
                    break;
                case LIDO_LITERAL:
                    if (caracter == '\'')
                        estado = EstadosEnum.LIDO_LITERAL_ERRO;
                    else if (caracter == ')' || caracter == ';' || caracter == ',' || caracter == '+'
                            || caracter == ' ') {
                        tksLidosOutroAnalisador.addAll(geraToken(palavra.substring(indice)));
                        palavra = palavra.substring(0, indice);
                        break analisador;
                    } else {
                        estado = EstadosEnum.LIDO_CARACTER_INVALIDO;
                        break analisador;
                    }
                    break;
                case LIDO_LITERAL_ERRO:
                    if (caracter == '\'') {
                        estado = EstadosEnum.LIDO_LITERAL;
                        palavra = palavra.replaceFirst("'", "");
                        palavra = palavra.replaceFirst("'", "#");
                        palavra = "'" + palavra;
                    } else {
                        estado = EstadosEnum.LIDO_CARACTER_INVALIDO;
                        break analisador;
                    }
                    break;
                case LIDO_NUMERO_REAL_INCOMPLETO:
                    if (Character.isDigit(caracter))
                        estado = EstadosEnum.LIDO_NUMERO_REAL;
                    else {
                        estado = EstadosEnum.LIDO_CARACTER_INVALIDO;
                        break analisador;
                    }
                    break;
                case LIDO_NUMERO_REAL:
                    if (Character.isDigit(caracter)) {
                        estado = EstadosEnum.LIDO_NUMERO_REAL;
                    } else {
                        if (Character.isLetter(caracter)) {
                            estado = EstadosEnum.LIDO_CARACTER_INVALIDO;
                        }
                        tksLidosOutroAnalisador.addAll(geraToken(palavra.substring(indice)));
                        palavra = palavra.substring(0, indice);
                        break analisador;
                    }
                    break;
                default:
                    estado = EstadosEnum.ESTADO_INVALIDO;
            }
            indice++;
        }

        Token tkAtual = TokenController.getToken(estado, palavra);
        if (tkAtual != null)
            tksEncontrados.add(TokenController.getToken(estado, palavra));
        tksEncontrados.addAll(tksLidosOutroAnalisador);
        return tksEncontrados;
    }
}
