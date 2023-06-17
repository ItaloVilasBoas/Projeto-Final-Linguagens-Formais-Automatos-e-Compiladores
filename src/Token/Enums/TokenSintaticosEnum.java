package Token.Enums;

public enum TokenSintaticosEnum implements TipoToken {
    PROGRAMA, BLOCO, DECLARACAO_VARIAVEIS, DECLARACAO_PROCEDIMENTO, COMANDO, COMANDO_COMPOSTO, PARAMETROS_FORMAIS,
    EXPRESSAO, LISTA_EXPRESSAO, CONDICIONAL, REPETITIVO, CHAMADA_PROCEDIMENTO, ERRO_SINTATICO, FIM_PROGRAMA;

    @Override
    public String getDescricao() {
        return this.name();
    }
}