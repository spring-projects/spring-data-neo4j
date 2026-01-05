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
package org.springframework.data.neo4j.integration.issues.gh2858;

import java.util.List;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Gerrit Meier
 */
@Node
public class GH2858 {

	@Id
	@GeneratedValue
	public String id;

	public String name;

	@Relationship("FRIEND_WITH")
	public List<GH2858> friends;

	@Relationship("RELATED_TO")
	public List<GH2858> relatives;

	/**
	 * Projection of GH2858 entity
	 */
	public interface GH2858Projection {

		String getName();

		List<Friend> getFriends();

		List<KnownPerson> getRelatives();

		/**
		 * Additional projection with just the name field.
		 */
		interface KnownPerson {

			String getName();

		}

		/**
		 * Additional projection with name field and friends relationship.
		 */
		interface Friend {

			String getName();

			List<KnownPerson> getFriends();

		}

	}

}
