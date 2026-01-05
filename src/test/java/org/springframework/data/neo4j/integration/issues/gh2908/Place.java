/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2908;

import org.neo4j.driver.Values;
import org.neo4j.driver.types.Point;

/**
 * Cool places to be.
 * @author Michael J. Simons
 */
public enum Place {

	NEO4J_HQ(Values.point(4326, 12.994823, 55.612191).asPoint()),
	SFO(Values.point(4326, -122.38681, 37.61649).asPoint()),
	CLARION(Values.point(4326, 12.994243, 55.607726).asPoint()),
	MINC(Values.point(4326, 12.994039, 55.611496).asPoint());

	private final Point value;

	Place(Point value) {
		this.value = value;
	}

	public Point getValue() {
		return value;
	}
}
