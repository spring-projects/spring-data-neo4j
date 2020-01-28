/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.integration.shared;

import java.util.concurrent.atomic.AtomicInteger;

import org.neo4j.driver.Driver;
import org.neo4j.springframework.data.core.schema.IdGenerator;
import org.springframework.util.StringUtils;

/**
 * This is a naive sequence generator which must not be used in production.
 *
 * @author Michael J. Simons
 */
public class TestSequenceGenerator implements IdGenerator<String> {

	private final AtomicInteger sequence = new AtomicInteger(0);

	/**
	 * Use an instance of the {@link Driver} bean here to ensure that also injection works when the {@link IdGenerator}
	 * gets created.
	 **/
	private final Driver driver;

	public TestSequenceGenerator(Driver driver) {
		this.driver = driver;
	}

	@Override
	public String generateId(String primaryLabel, Object entity) {
		return StringUtils.uncapitalize(primaryLabel) + "-" + sequence.incrementAndGet();
	}
}
