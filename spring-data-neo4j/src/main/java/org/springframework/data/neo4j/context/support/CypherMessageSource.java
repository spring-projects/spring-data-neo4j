/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */
package org.springframework.data.neo4j.context.support;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.LocaleUtils;
import org.neo4j.ogm.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * <p>
 * An implementation of {@link org.springframework.context.MessageSource} which loads messages from a graph database using Cypher, supporting basic localization and internationalization.
 * </p>
 * <p>
 * The Cypher query used to retrieve localized messages from the database can be overridden by using <code>setQueryCypher()</code>. Messages can be created within the database by using Cypher
 * statements such as: <br/>
 * <br/>
 * <blockquote>
 * 
 * <pre>
 * CREATE (n1:LocalizedMessage { code: 'goodbye', en_US: 'Goodbye', en_GB: 'Cheerio'})
 * </pre>
 * 
 * </blockquote>
 * </p>
 * <p>
 * Should your application make use of JavaConfig, this class can be registered with your <code>ApplicationContext</code> by using the following configuration:
 * </p>
 * <blockquote>
 * 
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
 * 
 * </blockquote>
 * 
 * @author Eric Spiegelberg - eric [at] miletwentyfour [dot] com
 */
@Service
public class CypherMessageSource extends AbstractMessageSource {

    @Autowired
    private Neo4jOperations neo4jOperations;

    private boolean initialized;

    private String queryCypher = "match (n:LocalizedMessage) return n.code as code, n.en_US as en_US, n.en_GB as en_GB";

    private Map<String, Map<Locale, String>> messages = new HashMap<String, Map<Locale, String>>();

    public void initialize() {

        Assert.notNull(neo4jOperations, "neo4jOperations must not be null");

        if (!initialized) {

            initialized = true;

            Map<String, Object> parameters = new HashMap<String, Object>();
            Result results = neo4jOperations.query(queryCypher, parameters);

            Iterable<Map<String, Object>> resultNodes = results.queryResults();

            for (Map<String, Object> nodeMap : resultNodes) {

                String code = nodeMap.remove("code").toString();

                Set<String> propertyKeys = nodeMap.keySet();

                for (String propertyKey : propertyKeys) {

                    String message = nodeMap.get(propertyKey).toString();

                    Locale locale = LocaleUtils.toLocale(propertyKey);
                    addMessage(code, locale, message);

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