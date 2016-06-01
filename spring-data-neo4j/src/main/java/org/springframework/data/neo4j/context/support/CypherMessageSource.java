/**
 * Copyright 2011-2016 the original author or authors.
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
package org.springframework.data.neo4j.context.support;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.LocaleUtils;
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * <p>
 * An implementation of {@link org.springframework.context.MessageSource}
 * which loads messages from a graph database using Cypher, 
 * supporting basic localization and internationalization.
 * </p>
 * <p>
 * The Cypher query used to retrieve localized messages from the database can be 
 * overridden by using <code>setQueryCypher()</code>. Messages can be created within 
 * the database by using Cypher statements such as:
 * <br/>
 * <br/>
 * <blockquote>
 * <pre>
 * CREATE (n1:LocalizedMessage { code: 'goodbye', en_US: 'Goodbye', en_GB: 'Cheerio'})
 * </pre>
 * </blockquote>
 * </p>
 * <p>
 * Should your application make use of JavaConfig, this class can be registered within 
 * your <code>ApplicationContext</code> by using the following configuration:
 * </p>
 * <blockquote>
 * <pre>
 * {@literal @}Bean
 *   public MessageSource messageSource() {
 *
 *        MessageSource cypherMessageSource = new CypherMessageSource();
 *
 *        return cypherMessageSource;
 *
 *    }
 * }
 * </pre>
 * </blockquote>
 * @author Eric Spiegelberg - eric [at] miletwentyfour [dot] com
 */
@Service
public class CypherMessageSource extends AbstractMessageSource {

    @Autowired
    private Neo4jOperations neo4jOperations;

    private boolean initialized;

    private String queryCypher = "match (n:LocalizedMessage) return n";

    private Map<String, Map<Locale, String>> messages = new HashMap<String, Map<Locale, String>>();

    public void initialize() {

        if (!initialized && neo4jOperations != null) {

            initialized = true;

            Map<String, Object> parameters = new HashMap<String, Object>();
            Result<Map<String, Object>> results = neo4jOperations.query(queryCypher, parameters);
            Iterator<Map<String, Object>> resultsIterator = results.iterator();
            
            while (resultsIterator.hasNext()) {
            
            	Map<String, Object> result = resultsIterator.next();

                Iterator<Entry<String, Object>> resultEntry = result.entrySet().iterator();

                while (resultEntry.hasNext()) {

                    Entry<String, Object> node = resultEntry.next();

                    Node n = (Node) node.getValue();
                    
                    String code = n.removeProperty("code").toString();
                    
                    Iterable<String> propertyKeys = n.getPropertyKeys();
                    
                    for (String propertyKey : propertyKeys) {
                    	
                    	String message = n.getProperty(propertyKey).toString();

                    	String localeString = propertyKey;
                        Locale locale = LocaleUtils.toLocale(localeString);

                        addMessage(code, locale, message);
                        
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