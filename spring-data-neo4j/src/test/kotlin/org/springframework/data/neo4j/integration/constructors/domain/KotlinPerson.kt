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
package org.springframework.data.neo4j.integration.constructors.domain

import org.neo4j.ogm.annotation.GeneratedValue
import org.neo4j.ogm.annotation.Id
import org.neo4j.ogm.annotation.Index
import org.neo4j.ogm.annotation.NodeEntity
import org.neo4j.ogm.annotation.Relationship

/**
 * @author Nicolas Mervaillie
 * @author Michael J. Simons
 */
@NodeEntity
data class KotlinPerson(
        @Id var name: String,
        @Relationship var friendships: List<KotlinFriendship> = ArrayList()) {

}

@NodeEntity
data class KotlinDataPerson(
        @Id @GeneratedValue var id: Long? = null,
        @Index(unique = true) var name: String
)

// Just a helper to omit the id constructor parameter completely when calling from Java.
fun newDataPerson(name: String) = KotlinDataPerson(name = name)
