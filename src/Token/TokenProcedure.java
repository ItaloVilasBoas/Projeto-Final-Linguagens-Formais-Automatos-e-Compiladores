package Token;

import java.util.List;

public class TokenProcedure extends TokenIdentificador {

    private List<TokenIdentificador> parametros;

    public TokenProcedure(Token token, List<TokenIdentificador> parametros) {
        super(token);
        this.parametros = parametros;
    }

    public Boolean chamaProcedure(List<TokenIdentificador> parametrosRecebidos) {

        for (Integer each = 0; each < parametros.size(); each++) {
            if (!parametrosRecebidos.get(each).comparaTipoDado(parametros.get(each))) {
                return false;
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

}
