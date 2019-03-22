/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.test;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 06.10.11
 */
public class DocumentingTestBaseTests extends DocumentingTestBase {
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
        assertEquals(String.format("<title>Documents DocumentingTestBase</title>%n"),createTitle());
    }
    @Test
    public void testParagraphs() {
        assertEquals(String.format("<para>%nThis documents the documenting test base%n</para>%n"+
                "<para>%nAnd this is a second paragraph%n</para>%n")
                ,createText());
    }
    @Test
    public void testCreateSnippet() {
        assertEquals(String.format("    {%n" +
                "        title=\"Documents DocumentingTestBase\";%n" +
                "    }%n" +
                "        snippetTitle = \"SnippetTitle\";%n"),collectSnippet());
    }
}
