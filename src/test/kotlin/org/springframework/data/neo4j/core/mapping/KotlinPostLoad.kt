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
