import java.util.*;

public class SyntaxAnalyzer {

    public static class ParseError {
        public final int line;
        public final String message;
        public ParseError(int line, String message) { this.line = line; this.message = message; }
    }

    public static class Declaration {
        public String typeName;
        public final List<Declarator> declarators = new ArrayList<>();
        public static class Declarator {
            public final String name;
            public final int line;
            public LexicalAnalyzer.TokenType initType = null;
            public String initLexeme = null;
            public Declarator(String name, int line) { this.name = name; this.line = line; }
        }
    }

    public static class ParseResult {
        public final List<Declaration> declarations = new ArrayList<>();
        public final List<ParseError> syntaxErrors = new ArrayList<>();
    }

    private final List<LexicalAnalyzer.Token> tokens;
    private int pos = 0;

    public SyntaxAnalyzer(List<LexicalAnalyzer.Token> tokens) {
        this.tokens = tokens;
    }

    private LexicalAnalyzer.Token peek() { return tokens.get(Math.min(pos, tokens.size()-1)); }
    private LexicalAnalyzer.Token consume() { return tokens.get(pos++); }
    private boolean isEOF() { return peek().type == LexicalAnalyzer.TokenType.EOF; }

    public ParseResult parseAll() {
        ParseResult res = new ParseResult();
        while (!isEOF()) {
            Declaration decl = parseOne(res);
            if (decl != null) res.declarations.add(decl);

            if (!isEOF() && peek().type != LexicalAnalyzer.TokenType.TYPE) {
                // skip tokens until semicolon
                while (!isEOF() && peek().type != LexicalAnalyzer.TokenType.SEMICOLON) pos++;
                if (!isEOF()) pos++;
            }
        }
        return res;
    }

    private Declaration parseOne(ParseResult res) {
        LexicalAnalyzer.Token t = peek();
        if (t.type != LexicalAnalyzer.TokenType.TYPE) {
            res.syntaxErrors.add(new ParseError(t.line, "Expected type but found '" + t.lexeme + "'"));
            pos++;
            return null;
        }

        Declaration decl = new Declaration();
        decl.typeName = consume().lexeme;

        while (true) {
            LexicalAnalyzer.Token idTok = peek();

            if (idTok.type != LexicalAnalyzer.TokenType.IDENTIFIER) {
                res.syntaxErrors.add(new ParseError(idTok.line, "Expected identifier but found '" + idTok.lexeme + "'"));
                // advance to comma or semicolon
                while (!isEOF() && peek().type != LexicalAnalyzer.TokenType.COMMA &&
                        peek().type != LexicalAnalyzer.TokenType.SEMICOLON) pos++;
            } else {
                Declaration.Declarator dec = new Declaration.Declarator(idTok.lexeme, idTok.line);
                consume();

                if (peek().type == LexicalAnalyzer.TokenType.EQUALS) {
                    consume();
                    LexicalAnalyzer.Token lit = peek();
                    if (isLiteralToken(lit.type)) {
                        dec.initType = mapLiteralToType(lit.type);
                        dec.initLexeme = lit.lexeme;
                        consume();
                    } else {
                        res.syntaxErrors.add(new ParseError(lit.line, "Expected literal but found '" + lit.lexeme + "'"));
                    }
                }
                decl.declarators.add(dec);
            }

            if (peek().type == LexicalAnalyzer.TokenType.COMMA) {
                consume();
                continue;
            } else if (peek().type == LexicalAnalyzer.TokenType.SEMICOLON) {
                consume();
                break;
            } else if (peek().type == LexicalAnalyzer.TokenType.EOF) {
                res.syntaxErrors.add(new ParseError(peek().line, "Unexpected end of file; missing ';'"));
                break;
            } else {
                res.syntaxErrors.add(new ParseError(peek().line, "Unexpected token '" + peek().lexeme + "'"));
                while (!isEOF() && peek().type != LexicalAnalyzer.TokenType.COMMA &&
                        peek().type != LexicalAnalyzer.TokenType.SEMICOLON) pos++;
                if (peek().type == LexicalAnalyzer.TokenType.COMMA) {
                    consume();
                    continue;
                }
                if (peek().type == LexicalAnalyzer.TokenType.SEMICOLON) {
                    consume();
                    break;
                }
            }
        }

        return decl;
    }

    private boolean isLiteralToken(LexicalAnalyzer.TokenType t) {
        return t == LexicalAnalyzer.TokenType.INT_LITERAL ||
                t == LexicalAnalyzer.TokenType.LONG_LITERAL ||
                t == LexicalAnalyzer.TokenType.FLOAT_LITERAL ||
                t == LexicalAnalyzer.TokenType.DOUBLE_LITERAL ||
                t == LexicalAnalyzer.TokenType.CHAR_LITERAL ||
                t == LexicalAnalyzer.TokenType.STRING_LITERAL ||
                t == LexicalAnalyzer.TokenType.BOOLEAN_LITERAL;
    }

    // Map literal token to a representative literal type (keeps it simple)
    private LexicalAnalyzer.TokenType mapLiteralToType(LexicalAnalyzer.TokenType lit) {
        return lit; // token types already correspond to literal kinds
    }
}