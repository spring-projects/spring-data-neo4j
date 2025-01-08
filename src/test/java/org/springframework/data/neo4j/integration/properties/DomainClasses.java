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
package org.springframework.data.neo4j.integration.properties;

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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Michael J. Simons
 * @soundtrack Metallica - Metallica
 */
final class DomainClasses {

	private DomainClasses() {
	}

	abstract static class BaseClass {

		private String knownProperty;

		public String getKnownProperty() {
			return this.knownProperty;
		}

		public void setKnownProperty(String knownProperty) {
			this.knownProperty = knownProperty;
		}
	}

	@Node
	static class IrrelevantSourceContainer {
		@Id
		@GeneratedValue
		private Long id;

		@Relationship(type = "RELATIONSHIP_PROPERTY_CONTAINER")
		RelationshipPropertyContainer relationshipPropertyContainer;

		IrrelevantSourceContainer(
				RelationshipPropertyContainer relationshipPropertyContainer) {
			this.relationshipPropertyContainer = relationshipPropertyContainer;
		}

		public Long getId() {
			return this.id;
		}

		public RelationshipPropertyContainer getRelationshipPropertyContainer() {
			return this.relationshipPropertyContainer;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setRelationshipPropertyContainer(RelationshipPropertyContainer relationshipPropertyContainer) {
			this.relationshipPropertyContainer = relationshipPropertyContainer;
		}
	}

	@Node
	static class DynRelSourc1 {
		@Id
		@GeneratedValue
		private Long id;

		@Relationship
		Map<String, List<RelationshipPropertyContainer>> rels = new HashMap<>();

		public Long getId() {
			return this.id;
		}

		public Map<String, List<RelationshipPropertyContainer>> getRels() {
			return this.rels;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setRels(Map<String, List<RelationshipPropertyContainer>> rels) {
			this.rels = rels;
		}
	}

	@Node
	static class DynRelSourc2 {
		@Id
		@GeneratedValue
		private Long id;

		@Relationship
		Map<String, RelationshipPropertyContainer> rels = new HashMap<>();

		public Long getId() {
			return this.id;
		}

		public Map<String, RelationshipPropertyContainer> getRels() {
			return this.rels;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setRels(Map<String, RelationshipPropertyContainer> rels) {
			this.rels = rels;
		}
	}

	@Node
	static class IrrelevantTargetContainer {
		@Id
		@GeneratedValue
		private Long id;
	}

	@RelationshipProperties
	static class RelationshipPropertyContainer extends BaseClass {

		private @RelationshipId Long id;

		@TargetNode
		private IrrelevantTargetContainer irrelevantTargetContainer;

		public Long getId() {
			return this.id;
		}

		public IrrelevantTargetContainer getIrrelevantTargetContainer() {
			return this.irrelevantTargetContainer;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setIrrelevantTargetContainer(IrrelevantTargetContainer irrelevantTargetContainer) {
			this.irrelevantTargetContainer = irrelevantTargetContainer;
		}
	}

	@Node
	static class SimpleGeneratedIDPropertyContainer extends BaseClass {

		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return this.id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Node
	static class SimpleGeneratedIDPropertyContainerWithVersion extends SimpleGeneratedIDPropertyContainer {

		@Version
		private Long version;

		public Long getVersion() {
			return this.version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}
	}

	@Node
	static class SimplePropertyContainer extends BaseClass {

		@Id
		private String id;

		public String getId() {
			return this.id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}

	@Node
	static class SimplePropertyContainerWithVersion extends SimplePropertyContainer {

		@Version
		private Long version;

		public Long getVersion() {
			return this.version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}
	}

	@Node
	static class WeirdSource {

		@Id
		@Property("id")
		@DateLong
		private Date myFineId;

		@Relationship(type = "ITS_COMPLICATED")
		IrrelevantTargetContainer irrelevantTargetContainer;

		WeirdSource(Date myFineId, IrrelevantTargetContainer irrelevantTargetContainer) {
			this.myFineId = myFineId;
			this.irrelevantTargetContainer = irrelevantTargetContainer;
		}

		public Date getMyFineId() {
			return this.myFineId;
		}

		public IrrelevantTargetContainer getIrrelevantTargetContainer() {
			return this.irrelevantTargetContainer;
		}

		public void setMyFineId(Date myFineId) {
			this.myFineId = myFineId;
		}

		public void setIrrelevantTargetContainer(IrrelevantTargetContainer irrelevantTargetContainer) {
			this.irrelevantTargetContainer = irrelevantTargetContainer;
		}
	}

	@Node
	static class LonelySourceContainer {
		@Id
		@GeneratedValue
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

		public Long getId() {
			return this.id;
		}

		public RelationshipPropertyContainer getSingle() {
			return this.single;
		}

		public List<RelationshipPropertyContainer> getMultiNull() {
			return this.multiNull;
		}

		public List<RelationshipPropertyContainer> getMultiEmpty() {
			return this.multiEmpty;
		}

		public Map<String, List<IrrelevantTargetContainer>> getDynNullList() {
			return this.dynNullList;
		}

		public Map<String, List<SimpleGeneratedIDPropertyContainer>> getDynEmptyList() {
			return this.dynEmptyList;
		}

		public Map<String, SimpleGeneratedIDPropertyContainerWithVersion> getDynNullSingle() {
			return this.dynNullSingle;
		}

		public Map<String, SimplePropertyContainer> getDynEmptySingle() {
			return this.dynEmptySingle;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setSingle(RelationshipPropertyContainer single) {
			this.single = single;
		}

		public void setMultiNull(List<RelationshipPropertyContainer> multiNull) {
			this.multiNull = multiNull;
		}

		public void setMultiEmpty(List<RelationshipPropertyContainer> multiEmpty) {
			this.multiEmpty = multiEmpty;
		}

		public void setDynNullList(Map<String, List<IrrelevantTargetContainer>> dynNullList) {
			this.dynNullList = dynNullList;
		}

		public void setDynEmptyList(Map<String, List<SimpleGeneratedIDPropertyContainer>> dynEmptyList) {
			this.dynEmptyList = dynEmptyList;
		}

		public void setDynNullSingle(Map<String, SimpleGeneratedIDPropertyContainerWithVersion> dynNullSingle) {
			this.dynNullSingle = dynNullSingle;
		}

		public void setDynEmptySingle(Map<String, SimplePropertyContainer> dynEmptySingle) {
			this.dynEmptySingle = dynEmptySingle;
		}
	}
}
