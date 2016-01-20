package org.springframework.data.neo4j.context.support;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.LocaleUtils;
import org.neo4j.ogm.session.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Simple implementation of {@link org.springframework.context.MessageSource}
 * which allows messages to be loaded from the graph database using Cypher. 
 * This MessageSource supports basic internationalization.
 * <br/>
 * The Cypher query used to retrieve localized text from the database can be 
 * overridden by using <code>setQueryCypher</code>.
 * 
 * @author Eric Spiegelberg eric <at> miletwentyfour [dot] com
 */
@Service
public class CypherMessageSource extends AbstractMessageSource {

    @Autowired
    private Neo4jOperations neo4jOperations;

    private boolean initialized;

    private String queryCypher = "match (n:LocalizedText) return n";

    private Map<String, Map<Locale, String>> messages = new HashMap<String, Map<Locale, String>>();

    public void initialize() {

        if (!initialized && neo4jOperations != null) {

            initialized = true;

            Map<String, Object> parameters = new HashMap<String, Object>();
            Result results = neo4jOperations.query(queryCypher, parameters, true);

            for (Map<String, Object> result : results) {

                Iterator<Entry<String, Object>> resultEntry = result.entrySet().iterator();

                while (resultEntry.hasNext()) {

                    Entry<String, Object> node = resultEntry.next();

                    @SuppressWarnings("unchecked")
                    Map<String, String> nodeProperties = (Map<String, String>) node.getValue();

                    String code = nodeProperties.remove("code");

                    for (Entry<String, String> nodeEntry : nodeProperties.entrySet()) {

                        String text = nodeEntry.getValue();

                        String localeString = nodeEntry.getKey();
                        Locale locale = LocaleUtils.toLocale(localeString);

                        addMessage(code, locale, text);
                    }
                }

            }

        }

    }

    /**
     * Associate the given message with the given code.
     *
     * @param code the lookup code
     * @param locale the locale that the message should be found within
     * @param text the message associated with this lookup code
     */
    public void addMessage(String code, Locale locale, String text) {

        Assert.notNull(text, "Text must not be null");
        Assert.notNull(code, "Code must not be null");
        Assert.notNull(locale, "Locale must not be null");

        initialize();

        Map<Locale, String> messagesByCode = messages.get(code);

        if (messagesByCode == null) {
            messagesByCode = new HashMap<Locale, String>();
            messages.put(code, messagesByCode);
        }

        messagesByCode.put(locale, text);

    }

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {

        initialize();

        Map<Locale, String> localeToTextMap = messages.get(code);
        String text = localeToTextMap.get(locale);

        return createMessageFormat(text, locale);

    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setUninitialized() {

        this.initialized = false;

    }

    public String getQueryCypher() {
        return queryCypher;
    }

    public void setQueryCypher(String queryCypher) {
        this.queryCypher = queryCypher;
    }

}