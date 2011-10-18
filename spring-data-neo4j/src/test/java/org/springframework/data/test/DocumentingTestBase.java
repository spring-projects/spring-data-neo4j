/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.test;


import org.junit.After;

import java.io.*;
import java.util.regex.Matcher;

import static org.junit.Assert.fail;

public abstract class DocumentingTestBase {

    private static final String DOCBOOK_DIR = "target/docbkx";
    private static final String SRC_TEST_JAVA = "src/test/java";
    protected String title;
    protected String snippetTitle;
    protected String snippet;
    protected String[] paragraphs={};

    @After
    public void outputDocs() throws IOException {
        final File directory = new File(docbookDirectory(),"snippets");
        if (directory.isFile() || !directory.exists() && !directory.mkdirs()) throw new RuntimeException("Could not create directory "+directory);
        final String name = getSnippetName();
        final PrintWriter writer = new PrintWriter(new FileWriter(getSnippetFileName(directory,name)));
        writer.write(createHeader());
        writer.write(createTitle());
        writer.write(createText());
        writer.write(createSnippet());
        writer.write(createFooter());
        writer.close();
    }

    private String getSnippetName() {
        final String className = getClass().getSimpleName();
        return className.replaceAll("Tests?$","");
    }

    private File docbookDirectory() {
        return new File (new File(determineRoot(),module()), DOCBOOK_DIR);
    }

    protected File getSnippetFileName(File directory, String name) {
        return new File(directory, name + ".xml");
    }

    protected String createFooter() {
        return String.format("</section>%n");
    }

    protected String createHeader() {
        return String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n" +
                "<!DOCTYPE section PUBLIC \"-//OASIS//DTD DocBook XML V4.4//EN\" \"http://www.oasis-open.org/docbook/xml/4.4/docbookx.dtd\">%n" +
                "<section>%n");
    }

    protected String createTitle() {
        return String.format("<title>%s</title>%n", title);
    }

    protected String createText() {
        StringBuilder result = new StringBuilder();
        for (String paragraph : paragraphs) {
            result.append(String.format("<para>%n%s%n</para>%n", paragraph));
        }
        return result.toString();
    }

    protected String createSnippet() {
        return String.format("<example>%n" +
                "           <title>%s</title>%n" +
                "           <programlisting language=\"java\"><![CDATA[%s]]></programlisting>%n" +
                "       </example>%n", snippetTitle, collectSnippet());
    }

    protected String collectSnippet() {
        try {
            final BufferedReader reader = new BufferedReader(new FileReader(getJavaFile()));
            String line;
            StringBuilder snippetText = new StringBuilder();
            boolean inSnippet = false;
            while ((line = reader.readLine()) != null) {
                if (line.matches(".*//.+SNIPPET\\s+.*\\b"+snippet+"\\b.*")) {
                    inSnippet = !inSnippet;
                    continue;
                }
                if (inSnippet) {
                    snippetText.append(line).append(String.format("%n"));
                }
            }
            reader.close();
            return snippetText.toString();
        } catch (IOException ioe) {
            throw new RuntimeException("Error generating snippet docs ", ioe);
        }
    }

    protected File getJavaFile() {
        final String javaFileName = getClass().getName().replaceAll("\\.", Matcher.quoteReplacement(File.separator)) + ".java";
        final File javaFile = new File(testSourceDirectory(), javaFileName);
        if (!javaFile.exists()) fail("Snippet File " + javaFile + " does not exist ");
        return javaFile;
    }

    private File testSourceDirectory() {
        return new File(new File(determineRoot(),module()),SRC_TEST_JAVA);
    }
    private File currentDirectory() {
        return new File(".").getAbsoluteFile().getParentFile();
    }

    protected String module() {
        return "spring-data-neo4j";
    }

    protected File determineRoot() {
        File currentDirectory = currentDirectory();
        if (new File(currentDirectory,module()).exists()) return currentDirectory;
        currentDirectory = currentDirectory.getParentFile();
        if (new File(currentDirectory,module()).exists()) return currentDirectory;
        throw new IllegalStateException("Can't determine root directory, started at "+currentDirectory());
    }
}
