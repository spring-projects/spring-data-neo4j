/*
 * Copyright 2011-2022 the original author or authors.
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

import org.springframework.data.annotation.Transient
import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship

/**
 * @author Michael J. Simons
 * @soundtrack Genesis - Invisible Touch
 */
@Node
abstract class AbstractKotlinBase(open val name: String) {

	@Id
	@GeneratedValue
	var id: Long? = null
}

/**
 * This class passes the name to the abstract base class but does not define a property named name for its own.
 */
@Node
class ConcreteNodeWithAbstractKotlinBase(name: String, val anotherProperty: String) : AbstractKotlinBase(name) {
}

/**
 * This class passes the name to the abstract base class and does define a property named name for its own. This property
 * must be made transient (another option is to do this in the base class, which is actually preferred. This is not done
 * here as the base class defines the property for a non-data class as well)
 */
@Node
data class ConcreteDataNodeWithAbstractKotlinBase(override @Transient val name: String, val anotherProperty: String) : AbstractKotlinBase(name) {
}

@Node
open class OpenKotlinBase(open val name: String?) {

	@Id
	@GeneratedValue
	var id: Long? = null
}

/**
 * This class passes the name to the open base class but does not define a property named name for its own.
 */
@Node
class ConcreteNodeWithOpenKotlinBase(name: String, val anotherProperty: String) : OpenKotlinBase(name) {
}

/**
 * This class passes the name to the open base class and does define a property named name for its own. This property
 * must be made transient (another option is to do this in the base class, which is actually preferred. This is not done
 * here as the base class defines the property for a non-data class as well)
 */
@Node
data class ConcreteDataNodeWithOpenKotlinBase(override @Transient val name: String, val anotherProperty: String) : OpenKotlinBase(name) {
}


@Node
interface KotlinMovie {
	val id: String
	val name: String
}


@Node
class KotlinCinema(@Id val id: String, val name: String,
				   @Relationship("Plays", direction = Relationship.Direction.OUTGOING) val plays: List<KotlinMovie>
)

@Node
class KotlinAnimationMovie(@Id override val id: String, override val name: String, val studio: String?) : KotlinMovie
