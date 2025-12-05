import java.util.*;
import java.util.stream.Collectors;

public class LexicalAnalyzer {

    public enum TokenType {
        TYPE, IDENTIFIER,
        INT_LITERAL, LONG_LITERAL, FLOAT_LITERAL, DOUBLE_LITERAL,
        CHAR_LITERAL, STRING_LITERAL, BOOLEAN_LITERAL,
        EQUALS, COMMA, SEMICOLON,
        EOF
    }

    public static class Token {
        public final TokenType type;
        public final String lexeme;
        public final int line;
        public Token(TokenType t, String lex, int line) {
            this.type = t; this.lexeme = lex; this.line = line;
        }
        @Override
        public String toString() {
            return String.format("%-14s %-20s line %d", type, lexeme, line);
        }
    }

    public static class LexError {
        public final int line;
        public final String message;
        public LexError(int line, String message) { this.line = line; this.message = message; }
    }

    public static class LexResult {
        public final List<Token> tokens = new ArrayList<>();
        public final List<LexError> errors = new ArrayList<>();
    }

    private final String input;
    private final int length;
    private int pos = 0;
    private int line = 1;

    private static final Set<String> TYPES = new HashSet<>(Arrays.asList(
            "byte","short","int","long","float","double","boolean","char","String"
    ));

    public LexicalAnalyzer(String input) {
        this.input = input == null ? "" : input;
        this.length = this.input.length();
    }

    public LexResult tokenize() {
        LexResult res = new LexResult();

        while (true) {
            skipWhitespaceAndComments();
            if (pos >= length) break;
            char c = peek();

            try {
                if (Character.isLetter(c) || c == '_') {
                    String id = readIdentifier();
                    if (TYPES.contains(id)) res.tokens.add(new Token(TokenType.TYPE, id, line));
                    else if ("true".equals(id) || "false".equals(id))
                        res.tokens.add(new Token(TokenType.BOOLEAN_LITERAL, id, line));
                    else
                        res.tokens.add(new Token(TokenType.IDENTIFIER, id, line));

                } else if (Character.isDigit(c) || (c == '-' && lookAheadIsDigit())) {
                    // numbers (possibly negative)
                    Token numTok = readNumberToken(res);
                    if (numTok != null) res.tokens.add(numTok);
                    // if tokenization detected an unrecoverable lexical error, stop lexing further tokens
                    if (!res.errors.isEmpty()) break;

                } else if (c == '"') {
                    String s = readStringLiteral(res);
                    if (s != null) res.tokens.add(new Token(TokenType.STRING_LITERAL, s, line));
                    if (!res.errors.isEmpty()) break;

                } else if (c == '\'') {
                    String ch = readCharLiteral(res);
                    if (ch != null) res.tokens.add(new Token(TokenType.CHAR_LITERAL, ch, line));
                    if (!res.errors.isEmpty()) break;

                } else {
                    switch (c) {
                        case '=': pos++; res.tokens.add(new Token(TokenType.EQUALS, "=", line)); break;
                        case ',': pos++; res.tokens.add(new Token(TokenType.COMMA, ",", line)); break;
                        case ';': pos++; res.tokens.add(new Token(TokenType.SEMICOLON, ";", line)); break;
                        default:
                            // any other single character that isn't recognized is a lexical error
                            res.errors.add(new LexError(line, "Unrecognized character '" + c + "'"));
                            pos++;
                            // stop lexing on unrecognized char — lexical phase must catch this early
                            break;
                    }
                    if (!res.errors.isEmpty()) break;
                }
            } catch (Exception ex) {
                res.errors.add(new LexError(line, "Lexical exception: " + ex.getMessage()));
                pos++;
                break;
            }
        }

        res.tokens.add(new Token(TokenType.EOF, "", line));
        return res;
    }

    private boolean lookAheadIsDigit() {
        return pos + 1 < length && Character.isDigit(input.charAt(pos + 1));
    }

    private void skipWhitespaceAndComments() {
        while (pos < length) {
            char c = input.charAt(pos);
            if (c == '\n') { line++; pos++; }
            else if (Character.isWhitespace(c)) pos++;
            else if (c == '/' && pos + 1 < length && input.charAt(pos + 1) == '/') {
                pos += 2;
                while (pos < length && input.charAt(pos) != '\n') pos++;
            } else if (c == '/' && pos + 1 < length && input.charAt(pos + 1) == '*') {
                pos += 2;
                while (pos + 1 < length &&
                        !(input.charAt(pos) == '*' && input.charAt(pos + 1) == '/')) {
                    if (input.charAt(pos) == '\n') line++;
                    pos++;
                    if (pos >= length) break;
                }
                if (pos + 1 < length) pos += 2;
            } else break;
        }
    }

    private char peek() { return input.charAt(pos); }

    private String readIdentifier() {
        int start = pos;
        pos++;
        while (pos < length) {
            char c = input.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_') pos++;
            else break;
        }
        return input.substring(start, pos);
    }

    /**
     * readNumberToken: supports:
     *  - optional leading '-'
     *  - integer (INT_LITERAL) or integer with L/l suffix (LONG_LITERAL)
     *  - decimal (DOUBLE_LITERAL) with optional D/d suffix
     *  - float with f/F suffix (FLOAT_LITERAL)
     *
     * If digits are immediately followed by letters that are not valid suffixes, report lexical error.
     */
    private Token readNumberToken(LexResult res) {
        int start = pos;
        boolean hasDot = false;

        if (peek() == '-') pos++;

        while (pos < length) {
            char c = input.charAt(pos);
            if (Character.isDigit(c)) pos++;
            else if (c == '.' && !hasDot) { hasDot = true; pos++; }
            else break;
        }

        // if next char is a letter:
        if (pos < length) {
            char s = input.charAt(pos);
            // valid suffixes: f F d D l L
            if (s == 'f' || s == 'F') {
                pos++;
                String lex = input.substring(start, pos);
                return new Token(hasDot ? TokenType.FLOAT_LITERAL : TokenType.FLOAT_LITERAL, lex, line);
            } else if (s == 'd' || s == 'D') {
                pos++;
                String lex = input.substring(start, pos);
                return new Token(TokenType.DOUBLE_LITERAL, lex, line);
            } else if (s == 'l' || s == 'L') {
                // only allowed for integer (no dot) and preceded by digits
                pos++;
                String lex = input.substring(start, pos);
                if (hasDot) {
                    res.errors.add(new LexError(line, "Invalid long literal with decimal point: " + lex));
                    return null;
                }
                return new Token(TokenType.LONG_LITERAL, lex, line);
            } else if (Character.isLetter(s) || s == '_') {
                // E.g. 123invalid — lexical error: identifier cannot start with digit and be attached to digits
                int errStart = pos;
                // consume the alphanumeric tail to advance pointer
                while (pos < length) {
                    char cc = input.charAt(pos);
                    if (Character.isLetterOrDigit(cc) || cc == '_') pos++;
                    else break;
                }
                String bad = input.substring(start, pos);
                res.errors.add(new LexError(line, "Invalid token (identifier starting with digit): " + bad));
                return null;
            }
        }

        String lex = input.substring(start, pos);
        if (lex.isEmpty()) {
            res.errors.add(new LexError(line, "Invalid number token at line"));
            return null;
        }

        if (hasDot) return new Token(TokenType.DOUBLE_LITERAL, lex, line);
        else return new Token(TokenType.INT_LITERAL, lex, line);
    }

    private String readStringLiteral(LexResult res) {
        int startLine = line;
        StringBuilder sb = new StringBuilder();
        pos++; // skip opening "
        boolean closed = false;
        while (pos < length) {
            char c = input.charAt(pos);
            if (c == '\\') {
                if (pos + 1 < length) {
                    sb.append(c).append(input.charAt(pos + 1));
                    pos += 2;
                } else {
                    res.errors.add(new LexError(startLine, "Unterminated string literal"));
                    pos++;
                    break;
                }
            } else if (c == '"') {
                pos++;
                closed = true;
                break;
            } else {
                if (c == '\n') line++;
                sb.append(c);
                pos++;
            }
        }
        if (!closed) res.errors.add(new LexError(startLine, "Unterminated string literal"));
        return sb.toString();
    }

    private static final Set<Character> VALID_CHAR_ESCAPES = new HashSet<>(Arrays.asList(
            'b','t','n','f','r','\'','"','\\'
    ));

    private String readCharLiteral(LexResult res) {
        int startLine = line;
        pos++; // skip opening '
        StringBuilder sb = new StringBuilder();
        boolean closed = false;
        while (pos < length) {
            char c = input.charAt(pos);
            if (c == '\\') {
                if (pos + 1 < length) {
                    sb.append(c).append(input.charAt(pos + 1));
                    pos += 2;
                } else {
                    res.errors.add(new LexError(startLine, "Unterminated char literal"));
                    pos++;
                    break;
                }
            } else if (c == '\'') {
                pos++;
                closed = true;
                break;
            } else {
                if (c == '\n') {
                    res.errors.add(new LexError(startLine, "Newline inside char literal"));
                    line++;
                    pos++;
                    break;
                }
                sb.append(c);
                pos++;
            }
        }
        if (!closed) {
            res.errors.add(new LexError(startLine, "Unterminated char literal"));
            return null;
        }
        String content = sb.toString();
        // validate: either single character or escape sequence like \n
        if (content.length() == 1) return content;
        if (content.length() == 2 && content.charAt(0) == '\\' && VALID_CHAR_ESCAPES.contains(content.charAt(1))) {
            return content;
        }
        res.errors.add(new LexError(startLine, "Invalid char literal: '" + content + "'"));
        return null;
    }
}