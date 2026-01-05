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
package org.springframework.data.neo4j.core.mapping

import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.PostLoad

interface KotlinBase {
	var baseName: String?

	fun bar();
}

interface KotlinA : KotlinBase {
	var ownAttr: String?
}

@Node("Base")
class KotlinBaseImpl : KotlinBase {
	@Id
	@GeneratedValue
	val id: Long? = null

	override var baseName: String? = null

	@PostLoad
	override fun bar() {
		baseName = "someValue"
	}
}

@Node("A")
class KotlinAImpl : KotlinA, KotlinBase by KotlinBaseImpl() {
	@Id
	@GeneratedValue
	val id: Long? = null

	override var ownAttr: String? = null
}
