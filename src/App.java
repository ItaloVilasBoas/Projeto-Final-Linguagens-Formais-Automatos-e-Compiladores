import Analisador.AnalisadorLexico;
import Analisador.AnalisadorSintatico;
import Analisador.TokenController;

public class App {

    public static AnalisadorLexico analisadorLexico = new AnalisadorLexico("sample.minipascal");
    public static TokenController tkController = new TokenController();
    public static Integer errosSintatico = 0;

    public static void main(String[] args) throws Exception {

        // Token.Token tk = AnalisadorLexico.proximoToken();
        // while (tk != null) {
        // System.out.println(tk);
        // tk = AnalisadorLexico.proximoToken();
        // }

        AnalisadorSintatico.start();
    }

}
