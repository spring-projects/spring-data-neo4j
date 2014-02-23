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

package org.springframework.data.neo4j.aspects.support;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.model.NonNullNamed;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ValidationException;

import static org.springframework.data.neo4j.aspects.Person.persistedPerson;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml"})
public class EntityPropertyValidationTests extends EntityTestBase {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private Neo4jTemplate template;

	@BeforeTransaction
	public void cleanDb() {
		Neo4jHelper.cleanDb(template);
    }

    @Test(expected = ValidationException.class)
    @Transactional
    public void shouldFailValidationOnTooLongName() {
        persistedPerson("Michael.........................", 35);
    }

    @Test(expected = ValidationException.class)
    @Transactional
    public void shouldFailValidationOnNegativeAge() {
        persistedPerson("Michael", -1);
    }
    @Test(expected = ValidationException.class)
    @Transactional
    public void shouldFailValidationOnBigAge() {
        persistedPerson("Michael", 110);
    }
    @Test(expected = ValidationException.class)
    @Transactional
    public void shouldFailValidationOnNullName() {
        template.save(new NonNullNamed());
    }
}
