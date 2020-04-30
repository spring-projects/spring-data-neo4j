/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.doc.springframework.data.docs.repositories.projection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Gerrit Meier
 */
// tag::projection.interface.bean-access[]
@Component
class NameBean {

	String getFullName(Person person) {
		return person.firstName + " " + person.lastName;
	}
}

// end::projection.interface.bean-access[]
// tag::projection.interface[]
interface NamesOnly {
// tag::projection.interface.closed-projection[]
	String getFirstName();
	String getLastName();
// end::projection.interface.closed-projection[]
// tag::projection.interface.open-projection[]

	@Value("#{target.firstName + ' ' + target.lastName}")
	String getFullName();
// end::projection.interface.open-projection[]
// tag::projection.interface.default-method[]

	default String getName() {
		return getFirstName().concat(" ").concat(getLastName());
	}
// end::projection.interface.default-method[]
// tag::projection.interface.bean-access[]

	@Value("#{@nameBean.getFullName(target)}")
	String getFullNameFromBean();
// end::projection.interface.bean-access[]
// tag::projection.interface.method-parameters[]

	@Value("#{args[0] + ' ' + target.firstname + '!'}")
	String getSalutation(String prefix);
// end::projection.interface.method-parameters[]
}
// end::projection.interface[]
