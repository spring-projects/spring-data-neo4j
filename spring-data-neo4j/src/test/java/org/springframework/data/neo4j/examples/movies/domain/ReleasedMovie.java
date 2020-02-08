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
package org.springframework.data.neo4j.examples.movies.domain;

import java.util.Date;

import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.typeconversion.DateLong;

/**
 * @author Michal Bachman
 */
// the fields here will move to Movie, this separate class exists temporarily after its tests pass
public class ReleasedMovie extends AbstractAnnotatedEntity {

	private String title;

	@Property(name = "cinemaRelease") private Date released;

	@DateLong private Date cannesRelease;

	public ReleasedMovie() {}

	public ReleasedMovie(String title, Date released, Date cannesRelease) {
		this.title = title;
		this.released = released;
		this.cannesRelease = cannesRelease;
	}

	public String getTitle() {
		return title;
	}

	public Date getReleased() {
		return released;
	}

	public Date getCannesRelease() {
		return cannesRelease;
	}
}
