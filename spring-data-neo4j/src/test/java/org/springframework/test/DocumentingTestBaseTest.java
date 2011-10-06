package org.springframework.test;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 06.10.11
 */
public class DocumentingTestBaseTest extends DocumentingTestBase {
    // SNIPPET x
    {
        title="Documents DocumentingTestBase";
    }
    // SNIPPET x

    {
        paragraphs = new String[] {"This documents the documenting test base","And this is a second paragraph"};
        snippet = "x";
        // SNIPPET x
        snippetTitle = "SnippetTitle";
        // SNIPPET x
    }

    @Test
    public void testTitle() {
        assertEquals("<title>Documents DocumentingTestBase</title>\n",createTitle());
    }
    @Test
    public void testParagraphs() {
        assertEquals("<para>\nThis documents the documenting test base\n</para>\n"+
                "<para>\nAnd this is a second paragraph\n</para>\n"
                ,createText());
    }
    @Test
    public void testCreateSnippet() {
        assertEquals("    {\n" +
                "        title=\"Documents DocumentingTestBase\";\n" +
                "    }\n" +
                "        snippetTitle = \"SnippetTitle\";\n",collectSnippet());
    }
}
