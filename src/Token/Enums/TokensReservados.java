package Token.Enums;

public enum TokensReservados implements TipoToken {
    // CONSTANTES
    TRUE("TRUE"),
    FALSE("FALSE"),

    // OPERADORES ARITMETICOS
    OPARITSOMA("+"),
    OPARITSUB("-"),
    OPARITMULT("*"),
    OPARITDIV("/"),
    OPARITDIVINT("DIV"),
    OPARITMOD("MOD"),

    // OPERADORES LOGICOS
    OPALOGOR("OR"),
    OPALOGAND("AND"),
    OPALOGXOR("XOR"),
    OPALOGNOT("NOT"),

    // OPERADORES RELACIONAIS
    OPRELGREAT(">"),
    OPRELLESS("<"),
    OPRELEQUAL("="),
    OPRELGEQUAL(">="),
    OPRELLEQUAL("<="),
    OPRELNEQUAL("<>"),
    OPRELIN("IN"),

    // FUNCOES ARITMETICAS
    // SQRT("SQRT"),
    // TRUNC("TRUNC"),
    // ROUND("ROUND"),
    // RANDOM("RANDOM"),
    // ABS("ABS"),

    // OPERADORES DE STRING FUNCOES E PROCEDURES
    LENGTH("LENGTH"),
    ORD("ORD"),
    CHR("CHR"),
    VAL("VAL"),
    STR("STR"),
    COPY("COPY"),

    // TIPOS DE DADO
    INTEGER("INTEGER"),
    REAL("REAL"),
    CHAR("CHAR"),
    STRING("STRING"),
    BOOLEAN("BOOLEAN"),
    TEXT("TEXT"),
    ARRAY("ARRAY"),
    OF("OF"),

    // PROGRAMA
    PROGRAM("PROGRAM"),
    INPUT("INPUT"),
    OUTPUT("OUTPUT"),
    BEGIN("BEGIN"),
    END("END"),
    ATRIBUICAO(":="),
    FOR("FOR"),
    TO("TO"),
    DOWNTO("DOWNTO"),
    DO("DO"),
    IF("IF"),
    THEN("THEN"),
    ELSE("ELSE"),
    WHILE("WHILE"),
    REPEAT("REPEAT"),
    UNTIL("UNTIL"),
    CASE("CASE"),
    FPROGRAM("."),
    SEPLISTA(","),
    DECTIPO(":"),
    FDEFIN(";"),
    LCOMENTK("{"),
    LCOMENTP("(*"),
    RCOMENTK("}"),
    RCOMENTP("*)"),
    LPAREN("("),
    RPAREN(")"),
    COMENTLIN("//"),

    // DECLARACAO
    CONST("CONST"),
    PROCEDURE("PROCEDURE"),
    VAR("VAR"),

    // INPUT / OUTPUT
    WRITE("WRITE"),
    READ("READ"),
    WRITELN("WRITELN"),
    READLN("READLN"),
    PRINT("PRINT");

    public final String descricao;

    private TokensReservados(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getDescricao() {
        return this.descricao;
    }
}
