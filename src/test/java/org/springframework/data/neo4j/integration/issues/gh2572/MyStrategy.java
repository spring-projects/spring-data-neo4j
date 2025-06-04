/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2572;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.data.neo4j.core.schema.IdGenerator;

/**
 * Not actually needed during the test, but added to recreate the issue scenario as much
 * as possible.
 *
 * @author Michael J. Simons
 */
public final class MyStrategy implements IdGenerator<String> {

	private final AtomicInteger sequence = new AtomicInteger(10);

	@Override
	public String generateId(String primaryLabel, Object entity) {
		return primaryLabel + this.sequence.incrementAndGet();
	}

}
