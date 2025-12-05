import java.util.*;

public class SemanticAnalyzer {

    public static class SemError {
        public final int line;
        public final String message;
        public SemError(int line, String message) { this.line = line; this.message = message; }
    }

    private static final Set<String> JAVA_KEYWORDS = new HashSet<>(Arrays.asList(
            "abstract","assert","boolean","break","byte","case","catch","char","class","const","continue",
            "default","do","double","else","enum","extends","final","finally","float","for","goto","if",
            "implements","import","instanceof","int","interface","long","native","new","package","private",
            "protected","public","return","short","static","strictfp","super","switch","synchronized","this",
            "throw","throws","transient","try","void","volatile","while"
    ));

    public static List<SemError> analyze(List<SyntaxAnalyzer.Declaration> decls) {
        List<SemError> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (SyntaxAnalyzer.Declaration d : decls) {
            for (SyntaxAnalyzer.Declaration.Declarator dec : d.declarators) {

                if (JAVA_KEYWORDS.contains(dec.name))
                    errors.add(new SemError(dec.line, "Invalid identifier (reserved): " + dec.name));

                if (seen.contains(dec.name))
                    errors.add(new SemError(dec.line, "Duplicate variable name: " + dec.name));

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

    private static boolean isCompatible(String typeName, LexicalAnalyzer.TokenType litType, String lexeme) {
        switch (typeName) {
            case "String":
                return litType == LexicalAnalyzer.TokenType.STRING_LITERAL;
            case "boolean":
                return litType == LexicalAnalyzer.TokenType.BOOLEAN_LITERAL;
            case "char":
                return litType == LexicalAnalyzer.TokenType.CHAR_LITERAL;
            case "float":
                return litType == LexicalAnalyzer.TokenType.FLOAT_LITERAL ||
                        litType == LexicalAnalyzer.TokenType.INT_LITERAL ||
                        litType == LexicalAnalyzer.TokenType.DOUBLE_LITERAL;
            case "double":
                return litType == LexicalAnalyzer.TokenType.DOUBLE_LITERAL ||
                        litType == LexicalAnalyzer.TokenType.FLOAT_LITERAL ||
                        litType == LexicalAnalyzer.TokenType.INT_LITERAL;
            case "byte":
            case "short":
            case "int":
            case "long":
                if (litType == LexicalAnalyzer.TokenType.INT_LITERAL) return true;
                if (litType == LexicalAnalyzer.TokenType.LONG_LITERAL) {
                    // long literal assigned to int types should be invalid (unless fits)
                    // keep simple: long not assignable to int types except explicit cast (so false)
                    return "long".equals(typeName);
                }
                if (litType == LexicalAnalyzer.TokenType.DOUBLE_LITERAL || litType == LexicalAnalyzer.TokenType.FLOAT_LITERAL) {
                    // numeric with decimal to integer types only if integral value
                    try {
                        double v = Double.parseDouble(stripSuffix(lexeme));
                        return v == Math.rint(v);
                    } catch (Exception e) { return false; }
                }
                return false;
            default:
                return false;
        }
    }

    private static String stripSuffix(String lexeme) {
        if (lexeme == null) return null;
        int n = lexeme.length();
        if (n > 0) {
            char last = lexeme.charAt(n-1);
            if (last == 'l' || last == 'L' || last == 'f' || last == 'F' || last == 'd' || last == 'D')
                return lexeme.substring(0, n-1);
        }
        return lexeme;
    }
}