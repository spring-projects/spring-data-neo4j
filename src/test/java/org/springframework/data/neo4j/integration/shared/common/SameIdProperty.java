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
package org.springframework.data.neo4j.integration.shared.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gerrit Meier
 */
public class SameIdProperty {
	/**
	 * @author Gerrit Meier
	 */
	@Node("Pod")
	@Data
	@With
	@NoArgsConstructor
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class PodEntity {
		@Id
		private String code;
	}

	/**
	 * @author Gerrit Meier
	 */
	@Node("Pol")
	@Data
	@With
	@NoArgsConstructor
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class PolEntity {
		@Id
		private String code;

		@Relationship(type = "ROUTES")
		private List<PodEntity> routes = new ArrayList<>();
	}

	/**
	 * @author Gerrit Meier
	 */
	@Node("PolWithRP")
	@Data
	@With
	@NoArgsConstructor
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class PolEntityWithRelationshipProperties {
		@Id
		private String code;

		@Relationship(type = "ROUTES")
		private List<RouteProperties> routes = new ArrayList<>();
	}

	/**
	 * @author Gerrit Meier
	 */
	@RelationshipProperties
	@Data
	@With
	@NoArgsConstructor
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class RouteProperties {

		@RelationshipId
		private Long id;

		private Double truck;
		private String truckCurrency;

		private Double ft20;
		private String ft20Currency;

		private Double ft40;
		private String ft40Currency;

		private Double ft40HC;
		private String ft40HCCurrency;


		@TargetNode
		private PodEntity pod;
	}
}
