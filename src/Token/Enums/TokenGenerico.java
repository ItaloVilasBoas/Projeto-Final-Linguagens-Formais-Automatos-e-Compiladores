package Token.Enums;

public enum TokenGenerico implements TipoToken {
    ERRO("TOKEN DE ERRO"),
    ID("IDENTIFICADOR"),
    VALORINT("VALOR INTEIRO"),
    VALOREAL("VALOR REAL"),
    VALORLIT("VALOR LITERAL"),
    FIMLEITURA("FIM LEITURA DE ARQUIVO");

    private final String descricao;

    private TokenGenerico(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getDescricao() {
        return this.descricao;
    }
}
