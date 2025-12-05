import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

/**
 * CompilerSimulator (Professor-ready)
 *
 * Improvements applied:
 * - Stage-locked analysis implemented correctly (Lexical -> Syntax -> Semantic)
 * - Lexical analysis runs once and fills Tokens area (syntax/semantic reuse cached tokens)
 * - Syntax uses cached tokens (no re-tokenizing)
 * - Semantic uses cached parse result (no re-parsing)
 * - Buttons show enabled (green) and disabled (gray) states visually
 * - Clear resets everything (flags, caches, UI)
 * - Token formatting improved for aligned display
 * - Fully commented and organized
 */
public class CompilerSimulator extends JFrame {

    private JTextArea codeArea;
    private JTextArea resultArea;
    private JTextArea tokenArea;

    private JButton openBtn, lexBtn, synBtn, semBtn, clearBtn;
    private String code = "";

    // stage flags
    private boolean lexPassed = false;
    private boolean synPassed = false;
    private boolean semPassed = false;

    // Caches to avoid re-running lexical/syntax repeatedly
    private List<Lexer.Token> cachedTokens = null;
    private Parser.ParseResult cachedParse = null;

    public CompilerSimulator() {
        setTitle("Compiler Analysis Simulator — Stage Locked");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setBackground(new Color(245, 247, 250));
        setContentPane(root);

        // Top Buttons
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setBackground(new Color(230, 240, 255));
        top.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 230)));

        openBtn = styledButton("Open File");
        lexBtn  = styledButton("Lexical Analysis");
        synBtn  = styledButton("Syntax Analysis");
        semBtn  = styledButton("Semantic Analysis");
        clearBtn= styledButton("Clear");

        top.add(openBtn); top.add(lexBtn);
        top.add(synBtn);  top.add(semBtn);
        top.add(clearBtn);
        root.add(top, BorderLayout.NORTH);

        // Center split
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.7);

        codeArea = new JTextArea();
        codeArea.setEditable(false);
        codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setBorder(BorderFactory.createTitledBorder("Code Text Area"));
        split.setLeftComponent(codeScroll);

        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.setBackground(new Color(250, 250, 252));

        resultArea = new JTextArea(12, 30);
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        JScrollPane resultScroll = new JScrollPane(resultArea);
        resultScroll.setBorder(BorderFactory.createTitledBorder("Result Text Area"));
        right.add(resultScroll, BorderLayout.NORTH);

        tokenArea = new JTextArea();
        tokenArea.setEditable(false);
        tokenArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane tokenScroll = new JScrollPane(tokenArea);
        tokenScroll.setBorder(BorderFactory.createTitledBorder("Tokens (lexical)"));
        right.add(tokenScroll, BorderLayout.CENTER);

        split.setRightComponent(right);
        root.add(split, BorderLayout.CENTER);

        // initial state: only Open enabled
        resetAllFlags();
        cachedTokens = null;
        cachedParse = null;
        setAllButtonsState(false);
        setButtonState(openBtn, true);   // open always available
        setVisible(true);

        // Actions
        openBtn.addActionListener(e -> openFileStrictMode());
        lexBtn.addActionListener(e -> runLexical());
        synBtn.addActionListener(e -> runSyntax());
        semBtn.addActionListener(e -> runSemantic());
        clearBtn.addActionListener(e -> clearAll());
    }

    // ================= UI Helpers =================
    private JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBackground(new Color(200, 200, 200));
        b.setForeground(Color.BLACK);
        b.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return b;
    }

    /**
     * Set a button visually enabled/disabled.
     * Enabled -> green with white text (clickable)
     * Disabled -> gray with black text (non-clickable)
     */
    private void setButtonState(JButton btn, boolean enabled) {
        btn.setEnabled(enabled);
        if (enabled) {
            btn.setBackground(new Color(60, 179, 75)); // green
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(Color.LIGHT_GRAY);
            btn.setForeground(Color.DARK_GRAY);
        }
    }

    /** Convenience to set group of main buttons */
    private void setAllButtonsState(boolean enabled) {
        setButtonState(lexBtn, enabled);
        setButtonState(synBtn, enabled);
        setButtonState(semBtn, enabled);
        setButtonState(clearBtn, enabled);
    }

    private void resetAllFlags() {
        lexPassed = false;
        synPassed = false;
        semPassed = false;
    }

    // ================ File Open (Strict Mode) =================
    /**
     * Strict Mode on open:
     * - Load file into code area
     * - Require Lexical to be run for the new file: lexPassed/synPassed/semPassed set to false
     * - Enable Lexical and Clear (and Open) only; disable Syntax & Semantic
     */
    private void openFileStrictMode() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Java or Text Files", "java", "txt"));
        int r = fc.showOpenDialog(this);

        if (r == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                code = new String(Files.readAllBytes(file.toPath())).replace("\r\n", "\n");
                codeArea.setText(code);
                resultArea.setText("");
                tokenArea.setText("");

                // Strict mode: reset analysis flags & caches for the new file (must re-run lexical)
                resetAllFlags();
                cachedTokens = null;
                cachedParse = null;

                // UI: Lexical + Clear + Open enabled; Syntax & Semantic disabled
                setAllButtonsState(false);
                setButtonState(lexBtn, true);
                setButtonState(clearBtn, true);
                setButtonState(openBtn, true);
                setButtonState(synBtn, false);
                setButtonState(semBtn, false);

                resultArea.setText("File loaded. Run Lexical Analysis to begin (level 1).");

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error reading file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // =================== LEXICAL ===================
    private void runLexical() {
        resultArea.setText("");
        tokenArea.setText("");

        // run lexer and cache tokens
        Lexer.LexResult lex = new Lexer(code).tokenize();
        this.cachedTokens = lex.tokens;

        // show tokens in neat aligned format
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-14s %-20s %s\n", "TOKEN TYPE", "LEXEME", "LINE"));
        sb.append(String.format("%-14s %-20s %s\n", "----------", "------", "----"));
        for (Lexer.Token t : lex.tokens) {
            // hide EOF token from the human tokens display (optional)
            if (t.type == Lexer.Type.EOF) continue;
            String lexemeDisplay = t.lexeme.replace("\n", "\\n").replace("\r", "\\r");
            sb.append(String.format("%-14s %-20s line %d\n", t.type, lexemeDisplay, t.line));
        }
        tokenArea.setText(sb.toString());

        if (lex.errors.isEmpty()) {
            lexPassed = true;
            synPassed = false;
            semPassed = false;
            resultArea.setText("Lexical Analysis phase passed. You may proceed to Syntax Analysis (level 2).");

            // UI transitions
            setButtonState(lexBtn, false);
            setButtonState(synBtn, true);
            setButtonState(semBtn, false);
            setButtonState(clearBtn, true);
            setButtonState(openBtn, true);

        } else {
            // display lexical errors
            StringBuilder esb = new StringBuilder("Lexical Analysis errors:\n");
            int i = 1;
            for (Lexer.LexError err : lex.errors) {
                esb.append(i++).append(". Line ").append(err.line).append(": ").append(err.message).append("\n");
            }
            resultArea.setText(esb.toString());

            // On lexical errors, stage not passed; downstream disabled
            lexPassed = false;
            synPassed = false;
            semPassed = false;

            setAllButtonsState(false);
            setButtonState(clearBtn, true);
            setButtonState(openBtn, true);
        }
    }

    // =================== SYNTAX ===================
    private void runSyntax() {
        resultArea.setText("");

        // ensure lexical was successfully run and tokens cached
        if (!lexPassed || cachedTokens == null) {
            resultArea.setText("Cannot run Syntax Analysis: Lexical Analysis must pass first.");
            setAllButtonsState(false);
            setButtonState(clearBtn, true);
            setButtonState(openBtn, true);
            return;
        }

        // parse using cached tokens (do not re-run lexical)
        Parser.ParseResult pRes = new Parser(cachedTokens).parseAll();
        this.cachedParse = pRes; // cache parse result for semantic stage

        if (pRes.syntaxErrors.isEmpty()) {
            synPassed = true;
            semPassed = false;
            resultArea.setText("Syntax Analysis phase passed. You may proceed to Semantic Analysis (level 3).");

            // UI transitions
            setButtonState(synBtn, false);
            setButtonState(semBtn, true);
            setButtonState(clearBtn, true);
            setButtonState(openBtn, true);
        } else {
            // show syntax errors
            StringBuilder esb = new StringBuilder("Syntax Analysis errors:\n");
            int i = 1;
            for (Parser.ParseError err : pRes.syntaxErrors) {
                esb.append(i++).append(". Line ").append(err.line).append(": ").append(err.message).append("\n");
            }
            resultArea.setText(esb.toString());

            // on syntax errors, cannot proceed to semantic
            synPassed = false;
            semPassed = false;

            setAllButtonsState(false);
            setButtonState(clearBtn, true);
            setButtonState(openBtn, true);
        }
    }

    // =================== SEMANTIC ===================
    private void runSemantic() {
        resultArea.setText("");

        // ensure syntax was successfully run and parse cached
        if (!synPassed || cachedParse == null) {
            resultArea.setText("Cannot run Semantic Analysis: Syntax Analysis must pass first.");
            setAllButtonsState(false);
            setButtonState(clearBtn, true);
            setButtonState(openBtn, true);
            return;
        }

        List<SemanticAnalyzer.SemError> semErrs = SemanticAnalyzer.analyze(cachedParse.declarations);

        if (semErrs.isEmpty()) {
            semPassed = true;
            resultArea.setText("Semantic Analysis phase passed. All levels passed!");

            setButtonState(semBtn, false);
            setButtonState(clearBtn, true);
            setButtonState(openBtn, true);
        } else {
            StringBuilder esb = new StringBuilder("Semantic Analysis errors:\n");
            int i = 1;
            for (SemanticAnalyzer.SemError s : semErrs) {
                esb.append(i++).append(". Line ").append(s.line).append(": ").append(s.message).append("\n");
            }
            resultArea.setText(esb.toString());

            semPassed = false;
            setAllButtonsState(false);
            setButtonState(clearBtn, true);
            setButtonState(openBtn, true);
        }
    }

    // =================== CLEAR ===================
    private void clearAll() {
        code = "";
        codeArea.setText("");
        resultArea.setText("");
        tokenArea.setText("");
        resetAllFlags();
        cachedTokens = null;
        cachedParse = null;

        // INITIAL STATE: only Open enabled
        setAllButtonsState(false);
        setButtonState(openBtn, true);
        setButtonState(clearBtn, true); // keep Clear available to let user clear UI
    }

    // ======================================================
    // ======================= LEXER ========================
    // ======================================================
    static class Lexer {

        enum Type {
            TYPE, IDENTIFIER,
            INT_LITERAL, FLOAT_LITERAL, DOUBLE_LITERAL,
            CHAR_LITERAL, STRING_LITERAL, BOOLEAN_LITERAL,
            EQUALS, COMMA, SEMICOLON,
            EOF, UNKNOWN
        }

        static class Token {
            Type type;
            String lexeme;
            int line;
            Token(Type t, String lex, int line) {
                this.type = t; this.lexeme = lex; this.line = line;
            }
        }

        static class LexError {
            int line;
            String message;
            LexError(int l, String msg) { line = l; message = msg; }
        }

        static class LexResult {
            List<Token> tokens = new ArrayList<>();
            List<LexError> errors = new ArrayList<>();
        }

        private final String input;
        private final int length;
        private int pos = 0;
        private int line = 1;

        private static final Set<String> TYPES = new HashSet<>(Arrays.asList(
                "byte","short","int","long","float","double","boolean","char","String"
        ));

        public Lexer(String input) {
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
                        if (TYPES.contains(id)) res.tokens.add(new Token(Type.TYPE, id, line));
                        else if ("true".equals(id) || "false".equals(id))
                            res.tokens.add(new Token(Type.BOOLEAN_LITERAL, id, line));
                        else res.tokens.add(new Token(Type.IDENTIFIER, id, line));

                    } else if (Character.isDigit(c) || (c == '-' && lookAheadIsDigit())) {
                        res.tokens.add(readNumberToken());

                    } else if (c == '"') {
                        String s = readStringLiteral(res);
                        if (s != null) res.tokens.add(new Token(Type.STRING_LITERAL, s, line));

                    } else if (c == '\'') {
                        String ch = readCharLiteral(res);
                        if (ch != null) res.tokens.add(new Token(Type.CHAR_LITERAL, ch, line));

                    } else {
                        switch (c) {
                            case '=': pos++; res.tokens.add(new Token(Type.EQUALS, "=", line)); break;
                            case ',': pos++; res.tokens.add(new Token(Type.COMMA, ",", line)); break;
                            case ';': pos++; res.tokens.add(new Token(Type.SEMICOLON, ";", line)); break;

                            default:
                                res.errors.add(new LexError(line, "Unrecognized character '" + c + "'"));
                                pos++;
                        }
                    }
                } catch (Exception ex) {
                    res.errors.add(new LexError(line, "Lexical exception: " + ex.getMessage()));
                    pos++;
                }
            }

            res.tokens.add(new Token(Type.EOF, "", line));
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

        private Token readNumberToken() {
            int start = pos;
            boolean hasDot = false;

            if (peek() == '-') pos++;

            while (pos < length) {
                char c = input.charAt(pos);
                if (Character.isDigit(c)) pos++;
                else if (c == '.' && !hasDot) { hasDot = true; pos++; }
                else break;
            }

            if (pos < length) {
                char s = input.charAt(pos);
                if (s == 'f' || s == 'F') {
                    pos++;
                    return new Token(Type.FLOAT_LITERAL, input.substring(start, pos), line);
                }
                if (s == 'd' || s == 'D') {
                    pos++;
                    return new Token(Type.DOUBLE_LITERAL, input.substring(start, pos), line);
                }
            }

            String lex = input.substring(start, pos);
            if (hasDot) return new Token(Type.DOUBLE_LITERAL, lex, line);
            else return new Token(Type.INT_LITERAL, lex, line);
        }

        private String readStringLiteral(LexResult res) {
            int startLine = line;
            StringBuilder sb = new StringBuilder();

            pos++;
            boolean closed = false;

            while (pos < length) {
                char c = input.charAt(pos);

                if (c == '\\') {
                    if (pos + 1 < length) {
                        sb.append(c).append(input.charAt(pos + 1));
                        pos += 2;
                    } else {
                        res.errors.add(new LexError(startLine,
                                "Unterminated string literal"));
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

            if (!closed)
                res.errors.add(new LexError(startLine, "Unterminated string literal"));

            return sb.toString();
        }

        private String readCharLiteral(LexResult res) {
            int startLine = line;
            pos++;
            StringBuilder sb = new StringBuilder();
            boolean closed = false;

            while (pos < length) {
                char c = input.charAt(pos);
                if (c == '\\') {
                    if (pos + 1 < length) {
                        sb.append(c).append(input.charAt(pos + 1));
                        pos += 2;
                    } else {
                        res.errors.add(new LexError(startLine,
                                "Unterminated char literal"));
                        pos++;
                        break;
                    }
                } else if (c == '\'') {
                    pos++;
                    closed = true;
                    break;
                } else {
                    if (c == '\n') {
                        res.errors.add(new LexError(startLine,
                                "Newline inside char literal"));
                        line++;
                        pos++;
                        break;
                    }
                    sb.append(c);
                    pos++;
                }
            }

            if (!closed)
                res.errors.add(new LexError(startLine, "Unterminated char literal"));

            return sb.toString();
        }
    }

    // ======================================================
    // ======================= PARSER =======================
    // ======================================================
    static class Parser {
        static class ParseError {
            int line;
            String message;
            ParseError(int l, String m) { line = l; message = m; }
        }

        static class ParseResult {
            List<Declaration> declarations = new ArrayList<>();
            List<ParseError> syntaxErrors = new ArrayList<>();
        }

        private final List<Lexer.Token> tokens;
        private int pos = 0;

        Parser(List<Lexer.Token> tokens) { this.tokens = tokens; }

        private Lexer.Token peek() { return tokens.get(Math.min(pos, tokens.size() - 1)); }
        private Lexer.Token consume() { return tokens.get(pos++); }
        private boolean isEOF() { return peek().type == Lexer.Type.EOF; }

        public ParseResult parseAll() {
            ParseResult res = new ParseResult();

            while (!isEOF()) {
                Declaration decl = parseOne(res);
                if (decl != null) res.declarations.add(decl);

                if (!isEOF() && peek().type != Lexer.Type.TYPE) {
                    while (!isEOF() && peek().type != Lexer.Type.SEMICOLON) pos++;
                    if (!isEOF()) pos++; // skip semicolon
                }
            }
            return res;
        }

        private Declaration parseOne(ParseResult res) {
            Lexer.Token t = peek();
            if (t.type != Lexer.Type.TYPE) {
                res.syntaxErrors.add(new ParseError(t.line,
                        "Expected type but found '" + t.lexeme + "'"));
                pos++;
                return null;
            }

            Declaration decl = new Declaration();
            decl.typeName = consume().lexeme;

            while (true) {
                Lexer.Token idTok = peek();

                if (idTok.type != Lexer.Type.IDENTIFIER) {
                    res.syntaxErrors.add(new ParseError(idTok.line,
                            "Expected identifier but found '" + idTok.lexeme + "'"));
                    while (!isEOF() && peek().type != Lexer.Type.COMMA &&
                            peek().type != Lexer.Type.SEMICOLON) pos++;
                } else {
                    Declaration.Declarator dec =
                            new Declaration.Declarator(idTok.lexeme, idTok.line);
                    consume();

                    if (peek().type == Lexer.Type.EQUALS) {
                        consume();
                        Lexer.Token lit = peek();
                        if (isLiteral(lit.type)) {
                            dec.initType = lit.type;
                            dec.initLexeme = lit.lexeme;
                            consume();
                        } else {
                            res.syntaxErrors.add(new ParseError(lit.line,
                                    "Expected literal but found '" + lit.lexeme + "'"));
                        }
                    }

                    decl.declarators.add(dec);
                }

                if (peek().type == Lexer.Type.COMMA) {
                    consume();
                    continue;
                } else if (peek().type == Lexer.Type.SEMICOLON) {
                    consume();
                    break;
                } else if (peek().type == Lexer.Type.EOF) {
                    res.syntaxErrors.add(new ParseError(peek().line,
                            "Unexpected end of file; missing ';'"));
                    break;
                } else {
                    res.syntaxErrors.add(new ParseError(peek().line,
                            "Unexpected token '" + peek().lexeme + "'"));
                    while (!isEOF() && peek().type != Lexer.Type.COMMA &&
                            peek().type != Lexer.Type.SEMICOLON) pos++;
                    if (peek().type == Lexer.Type.COMMA) {
                        consume();
                        continue;
                    }
                    if (peek().type == Lexer.Type.SEMICOLON) {
                        consume();
                        break;
                    }
                }
            }

            return decl;
        }

        private boolean isLiteral(Lexer.Type t) {
            return t == Lexer.Type.INT_LITERAL || t == Lexer.Type.FLOAT_LITERAL ||
                    t == Lexer.Type.DOUBLE_LITERAL || t == Lexer.Type.CHAR_LITERAL ||
                    t == Lexer.Type.STRING_LITERAL || t == Lexer.Type.BOOLEAN_LITERAL;
        }
    }

    // ======================================================
    // ====================== DECLARATION ===================
    // ======================================================
    static class Declaration {
        String typeName;
        List<Declarator> declarators = new ArrayList<>();

        static class Declarator {
            String name;
            int line;
            Lexer.Type initType = null;
            String initLexeme = null;

            Declarator(String name, int line) {
                this.name = name; this.line = line;
            }
        }
    }

    // ======================================================
    // =================== SEMANTIC ANALYZER =================
    // ======================================================
    static class SemanticAnalyzer {

        static class SemError {
            int line;
            String message;
            SemError(int l, String m) { line = l; message = m; }
        }

        private static final Set<String> JAVA_KEYWORDS = new HashSet<>(Arrays.asList(
                "abstract","assert","boolean","break","byte","case","catch","char","class","const","continue",
                "default","do","double","else","enum","extends","final","finally","float","for","goto","if",
                "implements","import","instanceof","int","interface","long","native","new","package","private",
                "protected","public","return","short","static","strictfp","super","switch","synchronized","this",
                "throw","throws","transient","try","void","volatile","while"
        ));

        public static List<SemError> analyze(List<Declaration> decls) {
            List<SemError> errors = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            for (Declaration d : decls) {
                for (Declaration.Declarator dec : d.declarators) {

                    if (JAVA_KEYWORDS.contains(dec.name))
                        errors.add(new SemError(dec.line,
                                "Invalid identifier (reserved): " + dec.name));

                    if (seen.contains(dec.name))
                        errors.add(new SemError(dec.line,
                                "Duplicate variable name: " + dec.name));

                    if (dec.initType != null) {
                        if (!isCompatible(d.typeName, dec.initType, dec.initLexeme))
                            errors.add(new SemError(dec.line,
                                    "Type mismatch: cannot assign " + dec.initType +
                                            "(" + dec.initLexeme + ") to " +
                                            d.typeName + " variable '" + dec.name + "'"));
                    }

                    seen.add(dec.name);
                }
            }

            return errors;
        }

        private static boolean isCompatible(String typeName, Lexer.Type litType, String lexeme) {

            switch (typeName) {

                case "String":
                    return litType == Lexer.Type.STRING_LITERAL;

                case "boolean":
                    return litType == Lexer.Type.BOOLEAN_LITERAL;

                case "char":
                    return litType == Lexer.Type.CHAR_LITERAL;

                case "float":
                    return litType == Lexer.Type.FLOAT_LITERAL ||
                            litType == Lexer.Type.INT_LITERAL ||
                            litType == Lexer.Type.DOUBLE_LITERAL;

                case "double":
                    return litType == Lexer.Type.DOUBLE_LITERAL ||
                            litType == Lexer.Type.FLOAT_LITERAL ||
                            litType == Lexer.Type.INT_LITERAL;

                case "byte":
                case "short":
                case "int":
                case "long":
                    return litType == Lexer.Type.INT_LITERAL ||
                            (litType == Lexer.Type.DOUBLE_LITERAL && isIntegralDouble(lexeme)) ||
                            (litType == Lexer.Type.FLOAT_LITERAL && isIntegralFloat(lexeme));

                default:
                    return false;
            }
        }

        private static boolean isIntegralDouble(String lex) {
            try {
                double v = Double.parseDouble(lex);
                return v == Math.rint(v);
            } catch (Exception e) { return false; }
        }

        private static boolean isIntegralFloat(String lex) {
            try {
                if (lex.endsWith("f") || lex.endsWith("F"))
                    lex = lex.substring(0, lex.length() - 1);
                float v = Float.parseFloat(lex);
                return v == Math.round(v);
            } catch (Exception e) { return false; }
        }
    }

    // ======================================================
    // ======================= MAIN =========================
    // ======================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(CompilerSimulator::new);
    }
}