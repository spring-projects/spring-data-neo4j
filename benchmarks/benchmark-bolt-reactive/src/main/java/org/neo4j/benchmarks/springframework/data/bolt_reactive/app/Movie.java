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
package org.neo4j.benchmarks.springframework.data.bolt_reactive.app;

import org.springframework.lang.Nullable;

public class Movie {

	private final Long id;

	private final String title;

	private final String tagline;

	public Movie(String title, String tagline) {
		this(null, title, tagline);
	}

	Movie(@Nullable Long id, String title, String tagline) {
		this.id = id;
		this.title = title;
		this.tagline = tagline;
	}

	public Long getId() {
		return id;
	}

	Movie withId(Long id) {
		return this.id == id ? this : new Movie(id, this.title, this.tagline);
	}

	public String getTitle() {
		return title;
	}

	public String getTagline() {
		return tagline;
	}
}
