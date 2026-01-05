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
package org.springframework.data.neo4j.integration.properties;

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
 */
final class DomainClasses {

	private DomainClasses() {
	}

	abstract static class BaseClass {

		private String knownProperty;

		String getKnownProperty() {
			return this.knownProperty;
		}

		void setKnownProperty(String knownProperty) {
			this.knownProperty = knownProperty;
		}

	}

	@Node
	static class IrrelevantSourceContainer {

		@Relationship(type = "RELATIONSHIP_PROPERTY_CONTAINER")
		RelationshipPropertyContainer relationshipPropertyContainer;

		@Id
		@GeneratedValue
		private Long id;

		IrrelevantSourceContainer(RelationshipPropertyContainer relationshipPropertyContainer) {
			this.relationshipPropertyContainer = relationshipPropertyContainer;
		}

		Long getId() {
			return this.id;
		}

		void setId(Long id) {
			this.id = id;
		}

		RelationshipPropertyContainer getRelationshipPropertyContainer() {
			return this.relationshipPropertyContainer;
		}

		void setRelationshipPropertyContainer(RelationshipPropertyContainer relationshipPropertyContainer) {
			this.relationshipPropertyContainer = relationshipPropertyContainer;
		}

	}

	@Node
	static class DynRelSourc1 {

		@Relationship
		Map<String, List<RelationshipPropertyContainer>> rels = new HashMap<>();

		@Id
		@GeneratedValue
		private Long id;

		Long getId() {
			return this.id;
		}

		void setId(Long id) {
			this.id = id;
		}

		Map<String, List<RelationshipPropertyContainer>> getRels() {
			return this.rels;
		}

		void setRels(Map<String, List<RelationshipPropertyContainer>> rels) {
			this.rels = rels;
		}

	}

	@Node
	static class DynRelSourc2 {

		@Relationship
		Map<String, RelationshipPropertyContainer> rels = new HashMap<>();

		@Id
		@GeneratedValue
		private Long id;

		Long getId() {
			return this.id;
		}

		void setId(Long id) {
			this.id = id;
		}

		Map<String, RelationshipPropertyContainer> getRels() {
			return this.rels;
		}

		void setRels(Map<String, RelationshipPropertyContainer> rels) {
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

		@RelationshipId
		private Long id;

		@TargetNode
		private IrrelevantTargetContainer irrelevantTargetContainer;

		Long getId() {
			return this.id;
		}

		void setId(Long id) {
			this.id = id;
		}

		IrrelevantTargetContainer getIrrelevantTargetContainer() {
			return this.irrelevantTargetContainer;
		}

		void setIrrelevantTargetContainer(IrrelevantTargetContainer irrelevantTargetContainer) {
			this.irrelevantTargetContainer = irrelevantTargetContainer;
		}

	}

	@Node
	static class SimpleGeneratedIDPropertyContainer extends BaseClass {

		@Id
		@GeneratedValue
		private Long id;

		Long getId() {
			return this.id;
		}

		void setId(Long id) {
			this.id = id;
		}

	}

	@Node
	static class SimpleGeneratedIDPropertyContainerWithVersion extends SimpleGeneratedIDPropertyContainer {

		@Version
		private Long version;

		Long getVersion() {
			return this.version;
		}

		void setVersion(Long version) {
			this.version = version;
		}

	}

	@Node
	static class SimplePropertyContainer extends BaseClass {

		@Id
		private String id;

		String getId() {
			return this.id;
		}

		void setId(String id) {
			this.id = id;
		}

	}

	@Node
	static class SimplePropertyContainerWithVersion extends SimplePropertyContainer {

		@Version
		private Long version;

		Long getVersion() {
			return this.version;
		}

		void setVersion(Long version) {
			this.version = version;
		}

	}

	@Node
	static class WeirdSource {

		@Relationship(type = "ITS_COMPLICATED")
		IrrelevantTargetContainer irrelevantTargetContainer;

		@Id
		@Property("id")
		@DateLong
		private Date myFineId;

		WeirdSource(Date myFineId, IrrelevantTargetContainer irrelevantTargetContainer) {
			this.myFineId = myFineId;
			this.irrelevantTargetContainer = irrelevantTargetContainer;
		}

		Date getMyFineId() {
			return this.myFineId;
		}

		void setMyFineId(Date myFineId) {
			this.myFineId = myFineId;
		}

		IrrelevantTargetContainer getIrrelevantTargetContainer() {
			return this.irrelevantTargetContainer;
		}

		void setIrrelevantTargetContainer(IrrelevantTargetContainer irrelevantTargetContainer) {
			this.irrelevantTargetContainer = irrelevantTargetContainer;
		}

	}

	@Node
	static class LonelySourceContainer {

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

		@Id
		@GeneratedValue
		private Long id;

		Long getId() {
			return this.id;
		}

		void setId(Long id) {
			this.id = id;
		}

		RelationshipPropertyContainer getSingle() {
			return this.single;
		}

		void setSingle(RelationshipPropertyContainer single) {
			this.single = single;
		}

		List<RelationshipPropertyContainer> getMultiNull() {
			return this.multiNull;
		}

		void setMultiNull(List<RelationshipPropertyContainer> multiNull) {
			this.multiNull = multiNull;
		}

		List<RelationshipPropertyContainer> getMultiEmpty() {
			return this.multiEmpty;
		}

		void setMultiEmpty(List<RelationshipPropertyContainer> multiEmpty) {
			this.multiEmpty = multiEmpty;
		}

		Map<String, List<IrrelevantTargetContainer>> getDynNullList() {
			return this.dynNullList;
		}

		void setDynNullList(Map<String, List<IrrelevantTargetContainer>> dynNullList) {
			this.dynNullList = dynNullList;
		}

		Map<String, List<SimpleGeneratedIDPropertyContainer>> getDynEmptyList() {
			return this.dynEmptyList;
		}

		void setDynEmptyList(Map<String, List<SimpleGeneratedIDPropertyContainer>> dynEmptyList) {
			this.dynEmptyList = dynEmptyList;
		}

		Map<String, SimpleGeneratedIDPropertyContainerWithVersion> getDynNullSingle() {
			return this.dynNullSingle;
		}

		void setDynNullSingle(Map<String, SimpleGeneratedIDPropertyContainerWithVersion> dynNullSingle) {
			this.dynNullSingle = dynNullSingle;
		}

		Map<String, SimplePropertyContainer> getDynEmptySingle() {
			return this.dynEmptySingle;
		}

		void setDynEmptySingle(Map<String, SimplePropertyContainer> dynEmptySingle) {
			this.dynEmptySingle = dynEmptySingle;
		}

	}

}
