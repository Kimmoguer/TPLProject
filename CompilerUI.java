import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.nio.file.Files;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class CompilerUI extends JFrame {

    private JTextArea codeArea;
    private JTextArea resultArea;
    private JTextArea tokenArea;

    private JButton openBtn, lexBtn, synBtn, semBtn, clearBtn;
    private String code = "";

    private boolean lexPassed = false;
    private boolean synPassed = false;
    private boolean semPassed = false;

    // caches
    private List<LexicalAnalyzer.Token> cachedTokens = null;
    private SyntaxAnalyzer.ParseResult cachedParse = null;
    private LexicalAnalyzer.LexResult lastLexResult = null;

    public CompilerUI() {
        setTitle("Compiler Analysis Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000,700);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBorder(new EmptyBorder(10,10,10,10));
        setContentPane(root);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        openBtn = styledButton("Open File");
        lexBtn = styledButton("Lexical Analysis");
        synBtn = styledButton("Syntax Analysis");
        semBtn = styledButton("Semantic Analysis");
        clearBtn = styledButton("Clear");
        top.add(openBtn); top.add(lexBtn); top.add(synBtn); top.add(semBtn); top.add(clearBtn);
        root.add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.7);

        codeArea = new JTextArea();
        codeArea.setEditable(false);
        codeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        JScrollPane cs = new JScrollPane(codeArea);
        cs.setBorder(BorderFactory.createTitledBorder("Code Text Area"));
        split.setLeftComponent(cs);

        JPanel right = new JPanel(new BorderLayout(8,8));
        resultArea = new JTextArea(12,30);
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        JScrollPane rs = new JScrollPane(resultArea);
        rs.setBorder(BorderFactory.createTitledBorder("Result Text Area"));
        right.add(rs, BorderLayout.NORTH);

        tokenArea = new JTextArea();
        tokenArea.setEditable(false);
        tokenArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane ts = new JScrollPane(tokenArea);
        ts.setBorder(BorderFactory.createTitledBorder("Tokens (lexical)"));
        right.add(ts, BorderLayout.CENTER);

        split.setRightComponent(right);
        root.add(split, BorderLayout.CENTER);

        resetAll();
        setVisible(true);

        // actions
        openBtn.addActionListener(e -> openFile());
        lexBtn.addActionListener(e -> doLexical());
        synBtn.addActionListener(e -> doSyntax());
        semBtn.addActionListener(e -> doSemantic());
        clearBtn.addActionListener(e -> resetAll());
    }

    private JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBackground(Color.LIGHT_GRAY);
        b.setForeground(Color.DARK_GRAY);
        b.setBorder(BorderFactory.createEmptyBorder(6,12,6,12));
        return b;
    }

    private void setButtonState(JButton btn, boolean enabled) {
        btn.setEnabled(enabled);
        if (enabled) {
            btn.setBackground(new Color(60,179,75));
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(Color.LIGHT_GRAY);
            btn.setForeground(Color.DARK_GRAY);
        }
    }

    private void resetAll() {
        code = "";
        codeArea.setText("");
        resultArea.setText("");
        tokenArea.setText("");
        cachedTokens = null;
        cachedParse = null;
        lastLexResult = null;
        lexPassed = synPassed = semPassed = false;

        setButtonState(openBtn, true);
        setButtonState(lexBtn, false);
        setButtonState(synBtn, false);
        setButtonState(semBtn, false);
        setButtonState(clearBtn, true);
    }

    private void openFile() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Java or Text Files", "java","txt"));
        int r = fc.showOpenDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                code = new String(Files.readAllBytes(f.toPath())).replace("\r\n", "\n");
                codeArea.setText(code);
                resultArea.setText("File loaded. Run Lexical Analysis to begin.");
                tokenArea.setText("");
                cachedTokens = null;
                cachedParse = null;
                lastLexResult = null;
                lexPassed = synPassed = semPassed = false;

                setButtonState(lexBtn, true);
                setButtonState(synBtn, false);
                setButtonState(semBtn, false);
                setButtonState(openBtn, true);
                setButtonState(clearBtn, true);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error reading file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void doLexical() {
        resultArea.setText("");
        tokenArea.setText("");
        if (code == null || code.trim().isEmpty()) {
            resultArea.setText("No file loaded. Use Open File first.");
            return;
        }

        LexicalAnalyzer lex = new LexicalAnalyzer(code);
        LexicalAnalyzer.LexResult r = lex.tokenize();
        lastLexResult = r;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-14s %-20s %s\n", "TOKEN TYPE", "LEXEME", "LINE"));
        sb.append(String.format("%-14s %-20s %s\n", "----------", "------", "----"));
        for (LexicalAnalyzer.Token t : r.tokens) {
            if (t.type == LexicalAnalyzer.TokenType.EOF) continue;
            sb.append(String.format("%-14s %-20s line %d\n", t.type, t.lexeme.replace("\n","\\n"), t.line));
        }
        tokenArea.setText(sb.toString());

        if (r.errors.isEmpty()) {
            lexPassed = true;
            cachedTokens = r.tokens.stream().collect(Collectors.toList());
            resultArea.setText("Lexical Analysis phase passed.");
            setButtonState(lexBtn, false);
            setButtonState(synBtn, true);
            setButtonState(semBtn, false);
        } else {
            lexPassed = false;
            cachedTokens = null;
            StringBuilder es = new StringBuilder("Lexical Analysis errors:\n");
            int i = 1;
            for (LexicalAnalyzer.LexError le : r.errors) {
                es.append(i++).append(". Line ").append(le.line).append(": ").append(le.message).append("\n");
            }
            resultArea.setText(es.toString());
            setButtonState(lexBtn, false);
            setButtonState(synBtn, false);
            setButtonState(semBtn, false);
        }
        setButtonState(openBtn, true);
        setButtonState(clearBtn, true);
    }

    private void doSyntax() {
        resultArea.setText("");
        if (!lexPassed || cachedTokens == null) {
            resultArea.setText("Cannot run Syntax Analysis: Lexical Analysis must pass first.");
            setButtonState(synBtn, false);
            setButtonState(semBtn, false);
            return;
        }

        SyntaxAnalyzer parser = new SyntaxAnalyzer(cachedTokens);
        SyntaxAnalyzer.ParseResult p = parser.parseAll();
        cachedParse = p;

        if (p.syntaxErrors.isEmpty()) {
            synPassed = true;
            resultArea.setText("Syntax Analysis phase passed.");
            setButtonState(synBtn, false);
            setButtonState(semBtn, true);
        } else {
            synPassed = false;
            StringBuilder es = new StringBuilder("Syntax Analysis errors:\n");
            int i = 1;
            for (SyntaxAnalyzer.ParseError pe : p.syntaxErrors) {
                es.append(i++).append(". Line ").append(pe.line).append(": ").append(pe.message).append("\n");
            }
            resultArea.setText(es.toString());
            setButtonState(synBtn, false);
            setButtonState(semBtn, false);
        }
        setButtonState(openBtn, true);
        setButtonState(clearBtn, true);
    }

    private void doSemantic() {
        resultArea.setText("");
        if (!synPassed || cachedParse == null) {
            resultArea.setText("Cannot run Semantic Analysis: Syntax Analysis must pass first.");
            return;
        }

        java.util.List<SemanticAnalyzer.SemError> semErrs = SemanticAnalyzer.analyze(cachedParse.declarations);
        if (semErrs.isEmpty()) {
            semPassed = true;
            resultArea.setText("Semantic Analysis phase passed. All levels passed!");
            setButtonState(semBtn, false);
        } else {
            semPassed = false;
            StringBuilder es = new StringBuilder("Semantic Analysis errors:\n");
            int i = 1;
            for (SemanticAnalyzer.SemError se : semErrs) {
                es.append(i++).append(". Line ").append(se.line).append(": ").append(se.message).append("\n");
            }
            resultArea.setText(es.toString());
            setButtonState(semBtn, false);
        }
        setButtonState(openBtn, true);
        setButtonState(clearBtn, true);
    }
}