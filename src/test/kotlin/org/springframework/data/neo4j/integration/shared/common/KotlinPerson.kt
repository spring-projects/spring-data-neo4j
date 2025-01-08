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

package org.springframework.data.neo4j.integration.shared.common

import org.springframework.data.neo4j.core.schema.*

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@Node
data class KotlinPerson(@Id @GeneratedValue val id: Long?, val name: String,
						@Relationship("WORKS_IN") val clubs: List<KotlinClubRelationship>) {
	constructor(name: String, clubs: List<KotlinClubRelationship>) : this(null, name, clubs)
}

@RelationshipProperties
data class KotlinClubRelationship(@RelationshipId val id: Long, val since: Int, @TargetNode val club: KotlinClub)

@Node
data class KotlinClub(@Id @GeneratedValue val id: Long, val name: String)
