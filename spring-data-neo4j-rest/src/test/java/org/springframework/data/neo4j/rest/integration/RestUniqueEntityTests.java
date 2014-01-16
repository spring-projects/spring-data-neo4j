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

package org.springframework.data.neo4j.rest.integration;

import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.data.neo4j.rest.support.RestTestBase;
import org.springframework.data.neo4j.unique.UniqueEntityTests;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

/**
 * @author mh
 * @since 28.03.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:unique-test-context.xml",
        "classpath:RestTests-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class RestUniqueEntityTests extends UniqueEntityTests {

    @BeforeClass
    public static void startDb() throws Exception {
        RestTestBase.startDb();
    }

    @Before
    public void cleanDb() {
        RestTestBase.cleanDb();
    }

    @AfterClass
    public static void shutdownDb() {
        RestTestBase.shutdownDb();

    }

    @Override
    @Ignore("Broken in Neo4j 2.0")
    @Test
    public void updatingToADuplicateValueShouldCauseAnException() {
    }

    @Override
    @Ignore("Broken in Neo4j 2.0")
    public void shouldOnlyCreateSingleInstanceForUniqueNodeEntity() {
    }

    @Override
    @Ignore("Broken in Neo4j 2.0")
    public void deletingUniqueNodeShouldRemoveItFromTheUniqueIndex() {
    }

    @Override
    @Ignore("Broken in Neo4j 2.0")
    public void shouldOnlyCreateSingleInstanceForUniqueNumericNodeEntity() {
    }

    @Override
    @Ignore("Broken in Neo4j 2.0")
    public void updatingToANewValueShouldAlsoUpdateTheIndex() {
    }

    @Override
    @Ignore("Broken in Neo4j 2.0")
    public void updatingToANewValueShouldKeepTheEntityUnique() {
    }
}
