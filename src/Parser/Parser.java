package Parser;

import Exceptions.Syntax.UnexpectedTokenException;
import Helpers.TagHelper;
import Lexer.Lexer;
import Lexer.Tokens.Tag;
import Lexer.Tokens.Token;
import Services.ErrorService;
import ast.ParameterNode;
import ast.ProgramNode;
import ast.declaration.DeclarationNode;
import ast.declaration.FunctionDeclarationNode;
import ast.declaration.ProcedureDeclarationNode;
import ast.declaration.TypeDeclarationNode;
import ast.statement.*;
import ast.type.SimpleTypeNode;
import ast.type.TypeNode;

import java.util.ArrayList;
import java.util.List;

public class Parser {

    private static Parser instance;
    private final ErrorService errorService;
    Lexer lexer;
    private Token currentToken;

    private Parser() {
        this.lexer = Lexer.getInstance();
        this.errorService = ErrorService.getInstance();
        this.currentToken = lexer.nextToken();
    }

    public static Parser getInstance() {
        if (!(instance == null)) {
            return instance;
        }
        instance = new Parser();
        return instance;
    }

    public ProgramNode parse() {
        return fichier();
    }

    private ProgramNode fichier() {
        ProgramNode abstractSyntaxTreeRoot = new ProgramNode();

        analyseTerminal(Tag.WITH);
        analyseTerminal(Tag.IDENT);
        analyseTerminal(Tag.DOT);
        analyseTerminal(Tag.IDENT);
        analyseTerminal(Tag.SEMICOLON);
        analyseTerminal(Tag.IDENT);
        analyseTerminal(Tag.DOT);
        analyseTerminal(Tag.IDENT);
        analyseTerminal(Tag.SEMICOLON);
        analyseTerminal(Tag.PROCEDURE);
        String rootProcedureName = currentToken.getValue();
        analyseTerminal(Tag.IDENT);
        analyseTerminal(Tag.IS);
        ProcedureDeclarationNode rootProcedure = new ProcedureDeclarationNode(rootProcedureName);
        BlockNode rootProcedureBody = new BlockNode();
        rootProcedureBody.setParent(rootProcedure);
        rootProcedureBody.addDeclarations(declarations());
        analyseTerminal(Tag.BEGIN);
        rootProcedureBody.addStatements(instrs());
        rootProcedure.setBody(rootProcedureBody);
        abstractSyntaxTreeRoot.setRootProcedure(rootProcedure);
        analyseTerminal(Tag.END);
        hasident();
        analyseTerminal(Tag.SEMICOLON);
        analyseTerminal(Tag.EOF);

        return abstractSyntaxTreeRoot;
    }

    private DeclarationNode declaration() {
        System.out.println("decl");
        DeclarationNode declaration;
        switch (this.currentToken.tag()) {
            case PROCEDURE -> {
                declaration = new ProcedureDeclarationNode(currentToken.getValue());
                analyseTerminal(Tag.PROCEDURE);
                analyseTerminal(Tag.IDENT);
                ((ProcedureDeclarationNode) declaration).addParameters(hasparams());
                analyseTerminal(Tag.IS);
                declarations();
                analyseTerminal(Tag.BEGIN);
                instrs();
                analyseTerminal(Tag.END);
                hasident();
                analyseTerminal(Tag.SEMICOLON);
            }
            case IDENT -> {
                declaration = new TypeDeclarationNode(currentToken.getValue());
                identsep();
                analyseTerminal(Tag.COLON);
                type_n();
                typexpr();
                analyseTerminal(Tag.SEMICOLON);
            }
            case TYPE -> {
                declaration = new TypeDeclarationNode(currentToken.getValue());
                analyseTerminal(Tag.TYPE);
                analyseTerminal(Tag.IDENT);
                hasischoose();
                analyseTerminal(Tag.SEMICOLON);
            }
            case FUNCTION -> {
                declaration = new FunctionDeclarationNode(currentToken.getValue());
                analyseTerminal(Tag.FUNCTION);
                analyseTerminal(Tag.IDENT);
                ((FunctionDeclarationNode) declaration).addParameters(hasparams());
                analyseTerminal(Tag.RETURN);
                type_n();
                analyseTerminal(Tag.IS);
                declarations();
                analyseTerminal(Tag.BEGIN);
                instrs();
                analyseTerminal(Tag.END);
                hasident();
            }
            default -> {
                declaration = null;
            }
        }
        return declaration;
    }

    private void hasischoose() {
        System.out.println("hasischoose");
        switch (this.currentToken.tag()) {
            case IS -> {
                analyseTerminal(Tag.IS);
                accorrec();
            }
            case SEMICOLON -> {
            }
        }
    }

    private void accorrec() {
        System.out.println("accorrec");
        switch (this.currentToken.tag()) {
            case ACCESS -> {
                analyseTerminal(Tag.ACCESS);
                analyseTerminal(Tag.IDENT);
            }
            case RECORD -> {
                analyseTerminal(Tag.RECORD);
                champs();
                analyseTerminal(Tag.END);
                analyseTerminal(Tag.RECORD);
            }
        }
    }

    private List<DeclarationNode> declarations() {
        System.out.println("decls");
        List<DeclarationNode> declarations = new ArrayList<>();
        switch (this.currentToken.tag()) {
            case PROCEDURE, IDENT, TYPE, FUNCTION -> {
                declarations.add(declaration());
                declarations.addAll(declarations());
            }
            case BEGIN -> {
            }
        }
        return declarations;
    }

    private void hasident() {
        System.out.println("hasident");
        switch (this.currentToken.tag()) {
            case SEMICOLON -> {
            }
            case IDENT -> analyseTerminal(Tag.IDENT);
        }
    }

    private void identsep() {
        System.out.println("identsep");
        if (this.currentToken.tag() == Tag.IDENT) {
            analyseTerminal(Tag.IDENT);
            identsep2();
        }
    }

    private void identsep2() {
        System.out.println("identsep2");
        switch (this.currentToken.tag()) {
            case COLON -> {
            }
            case COMMA -> {
                analyseTerminal(Tag.COMMA);
                identsep();
            }
        }
    }

    private void champ() {
        System.out.println("champ");
        if (this.currentToken.tag() == Tag.IDENT) {
            identsep();
            analyseTerminal(Tag.COLON);
            type_n();
            analyseTerminal(Tag.SEMICOLON);
        }
    }

    private void champs() {
        System.out.println("champs");
        if (this.currentToken.tag() == Tag.IDENT) {
            champ();
            champs2();
        }
    }

    private void champs2() {
        System.out.println("champs2");
        switch (this.currentToken.tag()) {
            case IDENT -> champs();
            case END -> {
            }
        }
    }

    private TypeNode type_n() {
        System.out.println("type_n");
        TypeNode type = null;
        switch (this.currentToken.tag()) {
            case ACCESS -> {
                analyseTerminal(Tag.ACCESS);
                analyseTerminal(Tag.IDENT);
            }
            case IDENT -> {
                type = new SimpleTypeNode();
                ((SimpleTypeNode) type).setTypeName(currentToken.getValue());
                analyseTerminal(Tag.IDENT);
            }
        }
        return type;
    }

    private List<ParameterNode> params() {
        System.out.println("params");
        List<ParameterNode> parameters = new ArrayList<>();
        if (this.currentToken.tag() == Tag.OPEN_PAREN) {
            analyseTerminal(Tag.OPEN_PAREN);
            parameters.addAll(paramsep());
            analyseTerminal(Tag.CLOSE_PAREN);
        }
        return parameters;
    }

    private List<ParameterNode> hasparams() {
        System.out.println("hasparams");
        List<ParameterNode> parameters = new ArrayList<>();
        switch (this.currentToken.tag()) {
            case IS, RETURN -> {
            }
            case OPEN_PAREN -> {
                parameters.addAll(params());
            }
        }
        return parameters;
    }

    private List<ParameterNode> paramsep() {
        System.out.println("paramsep");
        List<ParameterNode> parameters = new ArrayList<>();
        if (this.currentToken.tag() == Tag.IDENT) {
            parameters.add(param());
            parameters.addAll(paramsep2());
        }
        return parameters;
    }

    private List<ParameterNode> paramsep2() {
        System.out.println("paramsep2");
        List<ParameterNode> parameters = new ArrayList<>();
        switch (this.currentToken.tag()) {
            case SEMICOLON -> {
                analyseTerminal(Tag.SEMICOLON);
                parameters.addAll(paramsep());
            }
            case CLOSE_PAREN -> {
            }
        }
        return parameters;
    }

    private void typexpr() {
        System.out.println("typexpr");
        switch (this.currentToken.tag()) {
            case ASSIGN -> {
                analyseTerminal(Tag.ASSIGN);
                expr();
            }
            case SEMICOLON -> {
            }
        }
    }

    private ParameterNode param() {
        System.out.println("param");
        ParameterNode parameter = null;
        if (this.currentToken.tag() == Tag.IDENT) {
            parameter = new ParameterNode();
            parameter.setName(currentToken.getValue());
            identsep();
            analyseTerminal(Tag.COLON);
            mode();
            parameter.setType(type_n());
        }
        return parameter;
    }

    private void mode() {
        System.out.println("mode");
        switch (this.currentToken.tag()) {
            case IDENT, ACCESS -> {
            }
            case IN -> {
                analyseTerminal(Tag.IN);
                modeout();
            }

        }
    }

    private void modeout() {
        System.out.println("modeout");
        switch (this.currentToken.tag()) {
            case IDENT, ACCESS -> {
            }
            case OUT -> {
                analyseTerminal(Tag.OUT);
            }
        }
    }

    private void expr() {
        System.out.println("expr");
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, DOT, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
                or_expr();
            }

        }
    }

    private void or_expr() {
        System.out.println("or_expr");
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, DOT, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
                and_expr();
                or_expr2();
            }
        }
    }

    private void or_expr2() {
        System.out.println("or_expr2");
        switch (this.currentToken.tag()) {
            case SEMICOLON, COMMA, CLOSE_PAREN, THEN, DOTDOT, LOOP -> {
            }
            case OR -> {
                analyseTerminal(Tag.OR);
                or_expr3();
            }
        }
    }

    private void or_expr3() {
        System.out.println("or_expr3");
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, ELSE, DOT, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
                and_expr();
                or_expr2();
            }
        }
    }

    private void and_expr() {
        System.out.println("and_expr");
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
                not_expr();
                and_expr2();
            }

        }
    }

    private void and_expr2() {
        System.out.println("and_expr2");
        switch (this.currentToken.tag()) {
            case SEMICOLON, COMMA, CLOSE_PAREN, OR, THEN, DOTDOT, LOOP -> {
            }
            case AND -> {
                analyseTerminal(Tag.AND);
                and_expr3();
            }
        }
    }

    private void and_expr3() {
        System.out.println("and_expr3");
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
                not_expr();
                and_expr2();
            }
            case THEN -> {
                analyseTerminal(Tag.THEN);
                not_expr();
                and_expr2();
            }
        }
    }

    private void not_expr() {
        System.out.println("not_expr");
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
                equality_expr();
                not_expr2();
            }
        }
    }

    private void not_expr2() {
        System.out.println("not_expr2");
        switch (this.currentToken.tag()) {
            case SEMICOLON, COMMA, CLOSE_PAREN, OR, AND, THEN, DOTDOT, LOOP -> {
            }
            case NOT -> {
                analyseTerminal(Tag.NOT);
                equality_expr();
                not_expr2();
            }
        }
    }

    private void equality_expr() {
        System.out.println("equality_expr");
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
                relational_expr();
                equality_expr2();
            }
        }
    }

    private void equality_expr2() {
        System.out.println("equality_expr2");
        switch (this.currentToken.tag()) {
            case SEMICOLON, COMMA, CLOSE_PAREN, OR, AND, THEN, NOT, DOTDOT, LOOP -> {
            }
            case EQ -> {
                analyseTerminal(Tag.EQ);
                relational_expr();
                equality_expr2();
            }
            case NE -> {
                analyseTerminal(Tag.NE);
                relational_expr();
                equality_expr2();
            }
        }
    }

    private void relational_expr() {
        System.out.println("relational_expr");
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
                additive_expr();
                relational_expr2();
            }
        }
    }

    private void relational_expr2() {
        System.out.println("relational_expr2");
        switch (this.currentToken.tag()) {
            case SEMICOLON, COMMA, CLOSE_PAREN, OR, AND, THEN, NOT, EQ, NE, DOTDOT, LOOP -> {
            }
            case LT -> {
                analyseTerminal(Tag.LT);
                additive_expr();
                relational_expr2();
            }
            case LE -> {
                analyseTerminal(Tag.LE);
                additive_expr();
                relational_expr2();
            }
            case GT -> {
                analyseTerminal(Tag.GT);
                additive_expr();
                relational_expr2();
            }
            case GE -> {
                analyseTerminal(Tag.GE);
                additive_expr();
                relational_expr2();
            }
        }
    }

    private void additive_expr() {
        System.out.println("additive_expr");
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
                multiplicative_expr();
                additive_expr2();
            }
        }
    }

    private void additive_expr2() {
        System.out.println("additive_expr2");
        switch (this.currentToken.tag()) {
            case SEMICOLON, COMMA, CLOSE_PAREN, OR, AND, THEN, NOT, EQ, NE, LT, LE, GT, GE, DOTDOT, LOOP -> {
            }
            case PLUS -> {
                analyseTerminal(Tag.PLUS);
                multiplicative_expr();
                additive_expr2();
            }
            case MINUS -> {
                analyseTerminal(Tag.MINUS);
                multiplicative_expr();
                additive_expr2();
            }
        }
    }

    private void multiplicative_expr() {
        System.out.println("multiplicative_expr");
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
                unary_expr();
                multiplicative_expr2();
            }
        }
    }

    private void multiplicative_expr2() {
        System.out.println("multiplicative_expr2");
        switch (this.currentToken.tag()) {
            case SEMICOLON, COMMA, CLOSE_PAREN, OR, AND, THEN, NOT, EQ, NE, LT, LE, GT, GE, PLUS, MINUS, DOTDOT, LOOP -> {
            }
            case MULTI -> {
                analyseTerminal(Tag.MULTI);
                unary_expr();
                multiplicative_expr2();
            }
            case DIV -> {
                analyseTerminal(Tag.DIV);
                unary_expr();
                multiplicative_expr2();
            }
            case REM -> {
                analyseTerminal(Tag.REM);
                unary_expr();
                multiplicative_expr2();
            }
        }
    }

    private void unary_expr() {
        System.out.println("unary_expr");
        switch (this.currentToken.tag()) {
            case MINUS -> {
                analyseTerminal(Tag.MINUS);
                primary();
            }
            case IDENT, OPEN_PAREN, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
                primary();
            }
        }
    }

    private void primary() {
        System.out.println("primary");
        switch (this.currentToken.tag()) {
            case IDENT -> {
                analyseTerminal(Tag.IDENT);
                primary2();
            }
            case OPEN_PAREN -> {
                analyseTerminal(Tag.OPEN_PAREN);
                expr();
                analyseTerminal(Tag.CLOSE_PAREN);
            }
            case ENTIER -> {
                analyseTerminal(Tag.ENTIER);
            }
            case CARACTERE -> {
                analyseTerminal(Tag.CARACTERE);
            }
            case TRUE -> {
                analyseTerminal(Tag.TRUE);
            }
            case FALSE -> {
                analyseTerminal(Tag.FALSE);
            }
            case NULL -> {
                analyseTerminal(Tag.NULL);
            }
            case NEW -> {
                analyseTerminal(Tag.NEW);
                analyseTerminal(Tag.IDENT);
            }
            case CHARACTER -> {
                analyseTerminal(Tag.CHARACTER);
                analyseTerminal(Tag.APOSTROPHE);
                analyseTerminal(Tag.VAL);
                analyseTerminal(Tag.OPEN_PAREN);
                expr();
                analyseTerminal(Tag.CLOSE_PAREN);
            }

        }
    }

    private void primary2() {
        System.out.println("primary2");
        switch (this.currentToken.tag()) {
            case SEMICOLON, COMMA, CLOSE_PAREN, OR, AND, THEN, NOT, EQ, NE, LT, LE, GT, GE, PLUS, MINUS, MULTI, DIV, REM, DOTDOT, LOOP, DOT -> {
                acces();
            }
            case OPEN_PAREN -> {
                analyseTerminal(Tag.OPEN_PAREN);
                exprsep();
                analyseTerminal(Tag.CLOSE_PAREN);
                acces();
            }
        }
    }

    private void exprsep() {
        System.out.println("exprsep");
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
                expr();
                exprsep2();
            }
        }
    }

    private void exprsep2() {
        System.out.println("exprsep2");
        switch (this.currentToken.tag()) {
            case COMMA -> {
                analyseTerminal(Tag.COMMA);
                exprsep();
            }
            case CLOSE_PAREN -> {
            }
        }
    }

    private void hasexpr() {
        System.out.println("hasexpr");
        switch (this.currentToken.tag()) {
            case SEMICOLON -> {
            }
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
                expr();
            }
            case ASSIGN, DOT -> {
                exprsep();
            }
        }
    }

    private StatementNode instr() {
        System.out.println("instr");
        StatementNode statement;
        switch (this.currentToken.tag()) {
            case IDENT -> {
                // TODO ??
                statement = new BlockNode();
                analyseTerminal(Tag.IDENT);
                instr2();
            }
            case BEGIN -> {
                statement = new BlockNode();
                analyseTerminal(Tag.BEGIN);
                instrs();
                analyseTerminal(Tag.END);
                analyseTerminal(Tag.SEMICOLON);
            }
            case RETURN -> {
                statement = new ReturnStatementNode();
                analyseTerminal(Tag.RETURN);
                hasexpr();
                analyseTerminal(Tag.SEMICOLON);
            }
            case IF -> {
                statement = new IfStatementNode();
                analyseTerminal(Tag.IF);
                expr();
                analyseTerminal(Tag.THEN);
                instrs();
                elifn();
                elsen();
                analyseTerminal(Tag.END);
                analyseTerminal(Tag.IF);
                analyseTerminal(Tag.SEMICOLON);
            }
            case FOR -> {
                statement = new LoopStatementNode();
                analyseTerminal(Tag.FOR);
                analyseTerminal(Tag.IDENT);
                analyseTerminal(Tag.IN);
                hasreverse();
                expr();
                analyseTerminal(Tag.DOTDOT);
                expr();
                analyseTerminal(Tag.LOOP);
                instrs();
                analyseTerminal(Tag.END);
                analyseTerminal(Tag.LOOP);
                analyseTerminal(Tag.SEMICOLON);
            }
            case WHILE -> {
                statement = new LoopStatementNode();
                analyseTerminal(Tag.WHILE);
                expr();
                analyseTerminal(Tag.LOOP);
                instrs();
                analyseTerminal(Tag.END);
                analyseTerminal(Tag.LOOP);
                analyseTerminal(Tag.SEMICOLON);
            }
            default -> {
                statement = null;
            }
        }
        return statement;
    }

    private void instr2() {
        System.out.println("instr2");
        switch (this.currentToken.tag()) {
            case SEMICOLON -> {
                analyseTerminal(Tag.SEMICOLON);
            }
            case OPEN_PAREN -> {
                analyseTerminal(Tag.OPEN_PAREN);
                exprsep();
                analyseTerminal(Tag.CLOSE_PAREN);
                instr3();
                hasassign();
                analyseTerminal(Tag.SEMICOLON);
            }
            case ASSIGN, DOT -> {
                instr3();
                analyseTerminal(Tag.ASSIGN);
                expr();
                analyseTerminal(Tag.SEMICOLON);
            }
        }
    }

    private void instr3() {
        System.out.println("instr3");
        switch (this.currentToken.tag()) {
            case ASSIGN -> {
            }
            case DOT -> {
                analyseTerminal(Tag.DOT);
                analyseTerminal(Tag.IDENT);
                instr3();
            }
        }
    }

    private void hasassign() {
        System.out.println("hasassign");
        switch (this.currentToken.tag()) {
            case SEMICOLON -> {
            }
            case ASSIGN -> {
                analyseTerminal(Tag.ASSIGN);
                expr();
            }
        }
    }

    private void elifn() {
        System.out.println("elifn");
        switch (this.currentToken.tag()) {
            case END, ELSE -> {
            }
            case ELSIF -> {
                analyseTerminal(Tag.ELSIF);
                expr();
                analyseTerminal(Tag.THEN);
                instr();
                elifn();
            }
        }
    }

    private void elsen() {
        System.out.println("elsen");
        switch (this.currentToken.tag()) {
            case END -> {
            }
            case ELSE -> {
                analyseTerminal(Tag.ELSE);
                instrs();
            }
        }
    }

    private void hasreverse() {
        System.out.println("hasreverse");
        switch (this.currentToken.tag()) {
            case IDENT, OPEN_PAREN, MINUS, ENTIER, CARACTERE, TRUE, FALSE, NULL, NEW, CHARACTER -> {
            }
            case REVERSE -> {
                analyseTerminal(Tag.REVERSE);
            }
        }
    }

    private List<StatementNode> instrs() {
        System.out.println("instrs");
        List<StatementNode> statements = new ArrayList<>();
        switch (this.currentToken.tag()) {
            case IDENT, BEGIN, RETURN, IF, FOR, WHILE -> {
                statements.add(instr());
                statements.addAll(instrs2());
            }
        }

        //TODO
        return statements;
    }

    private List<StatementNode> instrs2() {
        System.out.println("instrs2");
        List<StatementNode> statements = new ArrayList<>();
        switch (this.currentToken.tag()) {
            case IDENT, BEGIN, RETURN, IF, FOR, WHILE -> {
                statements.add(instr());
                statements.addAll(instrs2());
            }
            case END, ELSE, ELSIF -> {
            }
        }
        return statements;
    }

    private void acces() {
        System.out.println("acces");
        switch (this.currentToken.tag()) {
            case SEMICOLON, COMMA, CLOSE_PAREN, OR, END, THEN, NOT, EQ, NE, LT, LE, GT, GE, PLUS, MINUS, MULTI, DIV, REM, DOTDOT, LOOP -> {
            }
            case DOT -> {
                analyseTerminal(Tag.DOT);
                analyseTerminal(Tag.IDENT);
                acces();
            }
        }

    }

    private void analyseTerminal(Tag tag) {
        System.out.println(this.currentToken);
        if (!(this.currentToken.tag() == tag)) {
            Token expectedToken = new Token(tag, this.currentToken.line(), TagHelper.getTagString(tag));
            this.errorService.registerSyntaxError(new UnexpectedTokenException(expectedToken, this.currentToken));
        }
        // Contient le prochain token ou <EOF, currentLine,""> si fin de fichier
        if (this.currentToken.tag() == Tag.EOF) {
            return;
        }
        this.currentToken = lexer.nextToken();
    }

}
