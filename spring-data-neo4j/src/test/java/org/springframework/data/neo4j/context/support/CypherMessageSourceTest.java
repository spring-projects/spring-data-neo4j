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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test coverage of the <code>CypherMessageSource</code>.
 * 
 * Both an integration and unit test are provided.
 * 
 * @author Eric Spiegelberg - eric [at] miletwentyfour [dot] com
 */
@Transactional
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class CypherMessageSourceTest {

	@Autowired
	private Neo4jTemplate neo4jTemplate;

	@Autowired
	private CypherMessageSource cypherMessageSource;

	/**
	 * This inner class uses JavaConfig to wire up the required dependencies and
	 * configuration.
	 */
	@Configuration
	@EnableNeo4jRepositories
	protected static class TestConfig extends Neo4jConfiguration {

		TestConfig() throws ClassNotFoundException {
			setBasePackage(getClass().getPackage().getName());
		}

		@Bean
		GraphDatabaseService graphDatabaseService() {
			return new TestGraphDatabaseFactory().newImpermanentDatabase();
		}

		@Bean
		public MessageSource messageSource() {

			CypherMessageSource cypherMessageSource = new CypherMessageSource();

			return cypherMessageSource;

		}

	}

	/**
	 * Reinitialize the CypherMessageSource between each test. This allows each
	 * test to populate the CypherMessageSource with its own desired messages.
	 */
	@Before
	public void setUp() {

		cypherMessageSource.setUninitialized();

	}

	@Test
	public void testLocalizedText() {

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
	 * An integration populating the <code>CypherMessageSource</code> from the
	 * impermanent database.
	 */
	@Test
	@Transactional
	public void testIntegration() {

		// Delete any previously existing LocalizedMessage's
		Map<String, Object> parameters = new HashMap<String, Object>(0);
		String deleteLocalizedMessagesCypher = "match (n:LocalizedMessage) delete n";
		neo4jTemplate.query(deleteLocalizedMessagesCypher, parameters);

		String hello = "Hello";
		String welcome = "Welcome";
		String save = "Save";
		String persist = "Persist";
		String goodbye = "Goodbye";
		String cherrio = "Cherrio";

		String createLocalizedMessage1Cypher = "CREATE (n1:LocalizedMessage { code: 'welcome', en_US: '" + welcome + "', en_GB: '" + hello + "'})";
		neo4jTemplate.query(createLocalizedMessage1Cypher, parameters);

		String createLocalizedMessage2Cypher = "CREATE (n1:LocalizedMessage { code: 'save', en_US: '" + save + "', en_GB: '" + persist + "'})";
		neo4jTemplate.query(createLocalizedMessage2Cypher, parameters);

		String createLocalizedMessage3Cypher = "CREATE (n1:LocalizedMessage { code: 'goodbye', en_US: '" + goodbye + "', en_GB: '" + cherrio + "'})";
		neo4jTemplate.query(createLocalizedMessage3Cypher, parameters);

		String args[] = new String[] {};

		String welcomeUS = cypherMessageSource.getMessage("welcome", args, Locale.US);
		Assert.assertEquals(welcome, welcomeUS);

		String welcomeUK = cypherMessageSource.getMessage("welcome", args, Locale.UK);
		Assert.assertEquals(hello, welcomeUK);

		String saveUS = cypherMessageSource.getMessage("save", args, Locale.US);
		Assert.assertEquals(save, saveUS);

		String saveUK = cypherMessageSource.getMessage("save", args, Locale.UK);
		Assert.assertEquals(persist, saveUK);

		String goodbyeUS = cypherMessageSource.getMessage("goodbye", args, Locale.US);
		Assert.assertEquals(goodbye, goodbyeUS);

		String goodbyeUK = cypherMessageSource.getMessage("goodbye", args, Locale.UK);
		Assert.assertEquals(cherrio, goodbyeUK);

	}

	/**
	 * Test/demonstrate the changing of the <code>CypherMessageSource</code> query.
	 */
	@Test
	@Transactional
	public void testSetQueryCypher() {

		// Delete any previously existing LocalizedMessage's
		Map<String, Object> parameters = new HashMap<String, Object>(0);
		String deleteLocalizedMessagesCypher = "match (n:LocalizedText) delete n";
		neo4jTemplate.query(deleteLocalizedMessagesCypher, parameters);

		String queryCypher = "match (n:LocalizedText) return n";
		cypherMessageSource.setQueryCypher(queryCypher);
		
		String hello = "Hello";
		String welcome = "Welcome";

		String createLocalizedMessage1Cypher = "CREATE (n1:n:LocalizedText { code: 'welcome', en_US: '" + welcome + "', en_GB: '" + hello + "'})";
		neo4jTemplate.query(createLocalizedMessage1Cypher, parameters);

		String args[] = new String[] {};

		String welcomeUS = cypherMessageSource.getMessage("welcome", args, Locale.US);
		Assert.assertEquals(welcome, welcomeUS);

	}

}