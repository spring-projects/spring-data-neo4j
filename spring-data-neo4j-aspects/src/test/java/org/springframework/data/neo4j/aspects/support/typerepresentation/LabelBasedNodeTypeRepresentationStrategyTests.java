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

package org.springframework.data.neo4j.aspects.support.typerepresentation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.neo4j.support.typerepresentation.LabelBasedNodeTypeRepresentationStrategy;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests to ensure that all scenarios involved in entity creation / reading etc
 * behave as expected, specifically where the Label Type Representation Strategy
 * is being used.
 *
 * The common scenarios/tests are defined in the superclass and each subclass, which
 * represents a specific strategy, needs to ensure that all is when then they
 * are used
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTests-context.xml",
        "classpath:org/springframework/data/neo4j/aspects/support/LabelingTypeRepresentationStrategyOverride-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class LabelBasedNodeTypeRepresentationStrategyTests extends AbstractNodeTypeRepresentationStrategyTestBase {


    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        assertThat("The tests in this class should be configured to use the Label " +
                   "based Type Representation Strategy, however it is not ... ",
                   nodeTypeRepresentationStrategy,
                   instanceOf(LabelBasedNodeTypeRepresentationStrategy.class));
    }

	@Test
	@Transactional
	public void testPostEntityCreation() throws Exception {
        // Anything to test here??
	}

	@Test
    @Transactional
	public void testPreEntityRemoval() throws Exception {
        // preEntityRemoval is a no op method, so nothing to test here!
	}

    @Test
    @Override
    public void testAssertLabelIndexOrNot() throws Exception {
        assertTrue("label based", nodeTypeRepresentationStrategy.isLabelBased());
    }
}
