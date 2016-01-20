package org.springframework.data.neo4j.context.support;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CypherMessageSourceTest {

    private CypherMessageSource cypherMessageSource;

    @Before
    public void setUp() {
        cypherMessageSource = new CypherMessageSource();
    }

    @Test
    public void testLocalizedText() {

        String code = "welcome";

        String textUS = "Welcome";
        cypherMessageSource.addMessage(code, Locale.US, textUS);
        String localizedText = cypherMessageSource.getMessage(code, new Object[] {}, Locale.US);
        Assert.assertEquals(textUS, localizedText);

        localizedText = cypherMessageSource.getMessage(code, new Object[] {}, Locale.UK);
        Assert.assertEquals("", localizedText);
        String textUK = "Greetings";
        cypherMessageSource.addMessage(code, Locale.UK, textUK);
        localizedText = cypherMessageSource.getMessage(code, new Object[] {}, Locale.UK);
        Assert.assertEquals(textUK, localizedText);

    }

}