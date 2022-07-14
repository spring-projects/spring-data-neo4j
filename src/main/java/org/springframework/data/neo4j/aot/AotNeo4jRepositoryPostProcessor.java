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
package org.springframework.data.neo4j.aot;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.data.aot.AotRepositoryContext;
import org.springframework.data.aot.RepositoryRegistrationAotProcessor;
import org.springframework.data.aot.TypeContributor;

/**
 * @author Gerrit Meier
 * @since 7.0.0
 */
public class AotNeo4jRepositoryPostProcessor extends RepositoryRegistrationAotProcessor {

 	@Override
 	protected void contribute(AotRepositoryContext repositoryContext, GenerationContext generationContext) {
 		super.contribute(repositoryContext, generationContext);

 		repositoryContext.getResolvedTypes().stream().filter(Neo4jAotPredicates.IS_SIMPLE_TYPE.negate()).forEach(type -> {
 			TypeContributor.contribute(type, it -> true, generationContext);
 		});
 	}
 }
