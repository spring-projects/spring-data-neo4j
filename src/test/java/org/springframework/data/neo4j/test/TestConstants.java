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
package org.springframework.data.neo4j.test;

import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.SymbolicName;

/**
 * Constants for test purposes only.
 *
 * @author Gerrit Meier
 */
public class TestConstants {

	public static final SymbolicName NAME_OF_ROOT_NODE = Cypher.name("n");

}
