/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.integration.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.neo4j.core.support.DateLong;

/**
 * @author Michael J. Simons
 * @soundtrack Metallica - Metallica
 */
final class DomainClasses {

	private DomainClasses() {
	}

	@Getter @Setter
	abstract static class BaseClass {

		private String knownProperty;
	}

	@Node
	@Getter @Setter
	static class IrrelevantSourceContainer {
		@Id @GeneratedValue
		private Long id;

		@Relationship(type = "RELATIONSHIP_PROPERTY_CONTAINER")
		RelationshipPropertyContainer relationshipPropertyContainer;

		IrrelevantSourceContainer(
				RelationshipPropertyContainer relationshipPropertyContainer) {
			this.relationshipPropertyContainer = relationshipPropertyContainer;
		}
	}

	@Node
	@Getter @Setter
	static class DynRelSourc1 {
		@Id @GeneratedValue
		private Long id;

		@Relationship
		Map<String, List<RelationshipPropertyContainer>> rels = new HashMap<>();
	}

	@Node
	@Getter @Setter
	static class DynRelSourc2 {
		@Id @GeneratedValue
		private Long id;

		@Relationship
		Map<String, RelationshipPropertyContainer> rels = new HashMap<>();
	}

	@Node
	static class IrrelevantTargetContainer {
		@Id @GeneratedValue
		private Long id;
	}

	@RelationshipProperties
	@Getter @Setter
	static class RelationshipPropertyContainer extends BaseClass {

		private @RelationshipId	Long id;

		@TargetNode
		private IrrelevantTargetContainer irrelevantTargetContainer;
	}

	@Node
	@Getter @Setter
	static class SimpleGeneratedIDPropertyContainer extends BaseClass {

		@Id @GeneratedValue
		private Long id;
	}

	@Node
	@Getter @Setter
	static class SimpleGeneratedIDPropertyContainerWithVersion extends SimpleGeneratedIDPropertyContainer {

		@Version
		private Long version;
	}

	@Node
	@Getter @Setter
	static class SimplePropertyContainer extends BaseClass {

		@Id
		private String id;
	}

	@Node
	@Getter @Setter
	static class SimplePropertyContainerWithVersion extends SimplePropertyContainer {

		@Version
		private Long version;
	}

	@Node
	@AllArgsConstructor
	@Getter @Setter
	static class WeirdSource {

		@Id
		@Property("id")
		@DateLong
		private Date myFineId;

		@Relationship(type = "ITS_COMPLICATED")
		IrrelevantTargetContainer irrelevantTargetContainer;
	}

	@Node
	@Getter @Setter
	static class LonelySourceContainer {
		@Id @GeneratedValue
		private Long id;

		@Relationship(type = "RELATIONSHIP_PROPERTY_CONTAINER")
		RelationshipPropertyContainer single;

		@Relationship(type = "RELATIONSHIP_PROPERTY_CONTAINER_2")
		List<RelationshipPropertyContainer> multiNull;

		@Relationship(type = "RELATIONSHIP_PROPERTY_CONTAINER_3")
		List<RelationshipPropertyContainer> multiEmpty = new ArrayList<>();

		@Relationship
		Map<String, List<IrrelevantTargetContainer>> dynNullList;

		@Relationship
		Map<String, List<SimpleGeneratedIDPropertyContainer>> dynEmptyList = new HashMap<>();

		@Relationship
		Map<String, SimpleGeneratedIDPropertyContainerWithVersion> dynNullSingle;

		@Relationship
		Map<String, SimplePropertyContainer> dynEmptySingle = new HashMap<>();
	}
}
