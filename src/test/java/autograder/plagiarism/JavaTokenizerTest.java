package autograder.plagiarism;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaTokenizerTest {
    private final JavaTokenizer tokenizer = new JavaTokenizer();

    @Test
    void renamedIdentifiersAndChangedLiteralsProduceTheSameTokens() {
        assertEquals(texts("int total = count + 5;"), texts("int sum = items + 9;"));
        assertEquals(texts("String s = \"hello\";"), texts("String t = \"bye\";"));
    }

    @Test
    void keepsKeywordsAndOperatorsButNormalizesNamesAndLiterals() {
        assertEquals(List.of("for", "(", "int", "ID", "=", "NUM", ";", "ID", "<=", "ID", ";", "ID", "++", ")"),
                texts("for (int i = 0; i <= n; i++)"));
        assertEquals(List.of("ID", "+=", "CHR", ";"), texts("x += 'a';"));
        assertEquals(List.of("return", "ID", ">>>", "NUM", ";"), texts("return v >>> 2;"));
    }

    @Test
    void ignoresCommentsWhitespaceAndStringContents() {
        String plain = "int x = 1;\nreturn x;";
        String decorated = "// leading comment\nint   x = 1; /* block\ncomment */\n\treturn x; // trailing";
        assertEquals(texts(plain), texts(decorated));
        assertEquals(List.of("ID", "=", "STR", ";"), texts("s = \"not // a comment \\\" still string\";"));
        assertEquals(List.of("ID", "=", "STR", ";"), texts("s = \"\"\"\n  text block\n  \"\"\";"));
    }

    @Test
    void recordsLineNumbersAcrossCommentsAndTextBlocks() {
        List<Token> tokens = tokenizer.tokenize("/*\n\n*/\nint x;\nString s = \"\"\"\nabc\n\"\"\";\nreturn;");
        assertEquals(4, tokens.get(0).line());
        assertEquals("return", tokens.get(tokens.size() - 2).text());
        assertEquals(8, tokens.get(tokens.size() - 2).line());
    }

    @Test
    void numbersInVariousFormatsBecomeOneToken() {
        assertEquals(List.of("NUM", "NUM", "NUM", "NUM", "NUM"), texts("1.5e-3 0xFF 10_000L .5 2f"));
    }

    @Test
    void unterminatedLiteralsAndCommentsDoNotFail() {
        assertEquals(List.of("ID", "=", "STR"), texts("s = \"open"));
        assertEquals(List.of("ID"), texts("x /* never closed"));
    }

    private List<String> texts(String source) {
        return tokenizer.tokenize(source).stream().map(Token::text).toList();
    }
}
