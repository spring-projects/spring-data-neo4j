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

package org.springframework.data.neo4j.core;

public enum Direction {
	
	OUTGOING(org.neo4j.graphdb.Direction.OUTGOING), INCOMING(
			org.neo4j.graphdb.Direction.INCOMING), BOTH(
			org.neo4j.graphdb.Direction.BOTH);
	
	private org.neo4j.graphdb.Direction neo4jDirection;

	private Direction( org.neo4j.graphdb.Direction neo4jDirection ) {
		this.neo4jDirection = neo4jDirection;
	}
	
	public org.neo4j.graphdb.Direction toNeo4jDir() {
		return this.neo4jDirection;
	}

}
