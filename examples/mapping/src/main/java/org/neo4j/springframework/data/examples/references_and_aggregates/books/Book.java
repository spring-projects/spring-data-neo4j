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
package org.neo4j.springframework.data.examples.references_and_aggregates.books;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.springframework.data.core.schema.GeneratedValue;
import org.neo4j.springframework.data.core.schema.Id;
import org.springframework.util.Assert;

/**
 * @author Michael J. Simons
 */
public class Book {
	@Id @GeneratedValue
	private Long id;

	private final String title;
	private final Set<AuthorRef> authors = new HashSet<>();

	public Book(String title) {
		this.title = title;
	}

	public Book addAuthor(Author author) {
		authors.add(createAuthorRef(author));

		return this;
	}

	private static AuthorRef createAuthorRef(Author author) {

		Assert.notNull(author, "Author must not be null");
		Assert.notNull(author.getId(), "Author id, must not be null");

		AuthorRef authorRef = new AuthorRef(author.getId());
		return authorRef;
	}
}
