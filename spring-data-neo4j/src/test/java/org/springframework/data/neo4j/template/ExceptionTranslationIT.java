/*
 * Copyright 2011-2019 the original author or authors.
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

package org.springframework.data.neo4j.template;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.examples.movies.domain.Rating;
import org.springframework.data.neo4j.template.context.DataManipulationEventConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Mark Angrish
 */
@ContextConfiguration(classes = DataManipulationEventConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class ExceptionTranslationIT extends MultiDriverTestClass {

	@Autowired private Neo4jOperations neo4jTemplate;

	@Before
	public void setUp() {
		Assert.notNull(neo4jTemplate, "Session not properly wired in");
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	@Transactional
	public void testTemplateExceptionsAreIntercepted() {
		neo4jTemplate.loadAll(Rating.class, 0);
	}
}
