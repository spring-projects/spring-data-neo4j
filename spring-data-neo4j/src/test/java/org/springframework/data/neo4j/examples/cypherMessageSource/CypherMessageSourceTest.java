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
import org.junit.Before;
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
	CypherMessageSource cypherMessageSource;

	/**
	 * Reinitialize the CypherMessageSource between each test. This allows each
	 * test to populate the CypherMessageSource with its own desired messages.
	 */
	@Before
	public void setUp() {

		cypherMessageSource.setUninitialized();

	}

	@Test
	public void testLocalizedMessage() {

		String code = "description";

		String textUS = "This is the US description";
		cypherMessageSource.addMessage(code, Locale.US, textUS);
		String localizedText = cypherMessageSource.getMessage(code, new Object[] {}, Locale.US);
		Assert.assertEquals(textUS, localizedText);

		localizedText = cypherMessageSource.getMessage(code, new Object[] {}, Locale.UK);
		Assert.assertEquals("", localizedText);
		String textUK = "This is the UK description";
		cypherMessageSource.addMessage(code, Locale.UK, textUK);
		localizedText = cypherMessageSource.getMessage(code, new Object[] {}, Locale.UK);
		Assert.assertEquals(textUK, localizedText);

	}

	/**
	 * This integration test populates the underlying Neo4j instance with
	 * messages codes. The autowired <code>CypherMessageSource</code> is used to
	 * retreive the internationalized messages.
	 */
	@Test
	public void integrationTest() {

		Map<String, Object> params = new HashMap<String, Object>();

		// Delete any previously existing LocalizedMessage's
		Map<String, Object> parameters = new HashMap<String, Object>(0);
		String deleteLocalizedMessagesCypher = "match (n:LocalizedMessage) delete n";
		neo4jTemplate.query(deleteLocalizedMessagesCypher, parameters);

		String createCypher = "CREATE (n1:LocalizedMessage { code: 'hello', en_US: 'Hello', en_GB: 'Greetings'})";
		neo4jTemplate.query(createCypher, params);

		createCypher = "CREATE (n1:LocalizedMessage { code: 'goodbye', en_US: 'Goodbye', en_GB: 'Cheerio'})";
		neo4jTemplate.query(createCypher, params);

		String message = cypherMessageSource.getMessage("hello", new String[] {}, Locale.US);
		Assert.assertEquals("Hello", message);

		message = cypherMessageSource.getMessage("hello", new String[] {}, Locale.UK);
		Assert.assertEquals("Greetings", message);

		message = cypherMessageSource.getMessage("goodbye", new String[] {}, Locale.US);
		Assert.assertEquals("Goodbye", message);

		message = cypherMessageSource.getMessage("goodbye", new String[] {}, Locale.UK);
		Assert.assertEquals("Cheerio", message);

	}

}