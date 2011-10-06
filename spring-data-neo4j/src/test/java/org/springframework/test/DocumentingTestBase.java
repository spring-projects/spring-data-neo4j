package org.springframework.test;


import org.junit.After;

import java.io.*;

import static org.junit.Assert.fail;

public abstract class DocumentingTestBase {

    protected String title;
    protected String snippetTitle;
    protected String snippet;
    protected String[] paragraphs={};

    @After
    public void outputDocs() throws IOException {
        final File directory = new File("src/docbkx/snippets");
        if (directory.isFile() || !directory.exists() && !directory.mkdirs()) throw new RuntimeException("Could not create directory "+directory);
        final String name = getClass().getSimpleName();
        final PrintWriter writer = new PrintWriter(new FileWriter(getSnippetFileName(directory,name)));
        writer.write(createHeader());
        writer.write(createTitle());
        writer.write(createText());
        writer.write(createSnippet());
        writer.write(createFooter());
        writer.close();
    }

    protected File getSnippetFileName(File directory, String name) {
        return new File(directory, name + ".xml");
    }

    protected String createFooter() {
        return "</section>\n";
    }

    protected String createHeader() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE section PUBLIC \"-//OASIS//DTD DocBook XML V4.4//EN\" \"http://www.oasis-open.org/docbook/xml/4.4/docbookx.dtd\">\n" +
                "<section>\n";
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
        return String.format("<example>\n" +
                "           <title>%s</title>\n" +
                "           <programlisting language=\"java\"><![CDATA[%s]]></programlisting>\n" +
                "       </example>\n", snippetTitle, collectSnippet());
    }

    protected String collectSnippet() {
        try {
            final BufferedReader reader = new BufferedReader(new FileReader(getJavaFile()));
            String line;
            StringBuilder snippetText = new StringBuilder();
            boolean inSnippet = false;
            while ((line = reader.readLine()) != null) {
                if (line.matches(".*//.+SNIPPET\\s+"+snippet+".*")) {
                    inSnippet = !inSnippet;
                    continue;
                }
                if (inSnippet) {
                    snippetText.append(line).append("\n");
                }
            }
            reader.close();
            return snippetText.toString();
        } catch (IOException ioe) {
            throw new RuntimeException("Error generating snippet docs ", ioe);
        }
    }

    protected File getJavaFile() {
        final String javaFileName = getClass().getName().replaceAll("\\.", File.separator) + ".java";
        final File javaFile = new File(module() + "/src/test/java", javaFileName);
        if (!javaFile.exists()) fail("Snippet File " + javaFile + " does not exist ");
        return javaFile;
    }

    protected String module() {
        return "spring-data-neo4j";
    }

}
