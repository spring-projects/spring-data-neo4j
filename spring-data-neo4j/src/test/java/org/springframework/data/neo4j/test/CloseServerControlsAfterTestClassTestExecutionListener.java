/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.test;

import static org.springframework.data.neo4j.test.Neo4jTestServerConfiguration.*;

import org.neo4j.harness.ServerControls;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * This tests execution listener takes care of closing Neo4j when the test context gets removed.
 * The shutdown hook will not apply until the JVM ends, so the tests won't close Neo4j even with
 * {@link org.springframework.test.annotation.DirtiesContext} on them. Therefor we do this manually.
 * As our doing actually also does dirty the context, we mark it accordingly.
 *
 * @author Michael J. Simons
 */
class CloseServerControlsAfterTestClassTestExecutionListener extends AbstractTestExecutionListener {

	@Override
	public void afterTestClass(TestContext testContext) {

		ApplicationContext applicationContext = testContext.getApplicationContext();
		if (applicationContext.containsBean(NEO4J_TEST_SERVER_BEAN_NAME)) {
			((ServerControls) applicationContext.getBean(NEO4J_TEST_SERVER_BEAN_NAME)).close();
			testContext.markApplicationContextDirty(null);
		}
	}
}
