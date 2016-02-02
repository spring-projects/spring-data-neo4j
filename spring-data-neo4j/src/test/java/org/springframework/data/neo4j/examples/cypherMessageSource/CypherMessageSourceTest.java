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

package org.springframework.data.neo4j.examples.cypherMessageSource;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.context.support.CypherMessageSource;
import org.springframework.data.neo4j.examples.cypherMessageSource.context.CypherMessageSourceContext;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test coverage of the <code>CypherMessageSource</code>.
 * 
 * @author Eric Spiegelberg - eric [at] miletwentyfour [dot] com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { CypherMessageSourceContext.class })
public class CypherMessageSourceTest extends MultiDriverTestClass {

    @Autowired
    Session session;

    @Autowired
    Neo4jOperations neo4jTemplate;

    @Autowired
    CypherMessageSource messageSource;

    @Test
    public void unitTest() {

    }

    /**
     * This integration test populates the underlying Neo4j instance with messages codes. The 
     * autowired <code>CypherMessageSource</code> is used to retreive the internationalized messages.
     */
    @Test
    public void integrationTest() {

        Map<String, Object> params = new HashMap<String, Object>();

        String createCypher = "CREATE (n1:LocalizedMessage { code: 'hello', en_US: 'Hello', en_GB: 'Greetings'})";
        neo4jTemplate.query(createCypher, params);

        createCypher = "CREATE (n1:LocalizedMessage { code: 'goodbye', en_US: 'Goodbye', en_GB: 'Cheerio'})";
        neo4jTemplate.query(createCypher, params);

        String message = messageSource.getMessage("hello", new String[] {}, Locale.US);
        Assert.assertEquals("Hello", message);

        message = messageSource.getMessage("hello", new String[] {}, Locale.UK);
        Assert.assertEquals("Greetings", message);

        message = messageSource.getMessage("goodbye", new String[] {}, Locale.US);
        Assert.assertEquals("Goodbye", message);

        message = messageSource.getMessage("goodbye", new String[] {}, Locale.UK);
        Assert.assertEquals("Cheerio", message);

    }

}