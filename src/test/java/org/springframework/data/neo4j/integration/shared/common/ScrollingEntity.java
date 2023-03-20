/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.integration.shared.common;

import java.time.LocalDateTime;
import java.util.UUID;

import org.neo4j.driver.QueryRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

/**
 * An entity that is specifically designed to test the keyset based pagination.
 *
 * @author Michael J. Simons
 */
@Node
public class ScrollingEntity {

	/**
	 * Sorting by b and a will not be unique for 3 and D0, so this will trigger the additional condition based on the id
	 */
	public static final Sort SORT_BY_B_AND_A = Sort.by(Sort.Order.asc("b"), Sort.Order.desc("a"));

	public static void createTestData(QueryRunner queryRunner) {
		queryRunner.run("MATCH (n) DETACH DELETE n");
		queryRunner.run("""
				UNWIND (range(0, 8) + [3]) AS i WITH i, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ' AS letters
				CREATE (n:ScrollingEntity {
					id: randomUUID(),
					foobar: (substring(letters, (toInteger(i) % 26), 1) + (i / 26)),
					b: i,
					c: (localdatetime() + duration({ days: i }) + duration({ seconds: i * toInteger(rand()*10) }))
				})
				RETURN n
				""");
	}

	@Id
	@GeneratedValue
	private UUID id;

	@Property("foobar")
	private String a;

	private Integer b;

	private LocalDateTime c;

	public UUID getId() {
		return id;
	}

	public String getA() {
		return a;
	}

	public void setA(String a) {
		this.a = a;
	}

	public Integer getB() {
		return b;
	}

	public void setB(Integer b) {
		this.b = b;
	}

	public LocalDateTime getC() {
		return c;
	}

	public void setC(LocalDateTime c) {
		this.c = c;
	}

	@Override
	public String toString() {
		return "ScrollingEntity{" +
				"a='" + a + '\'' +
				", b=" + b +
				", c=" + c +
				'}';
	}
}
