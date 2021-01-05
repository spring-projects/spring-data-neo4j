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
package org.springframework.data.neo4j.transactions.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author: Vince Bickers
 * @author Mark Angrish
 */
@Component
public class BusinessService {

	@Autowired private Session session;

	@Transactional
	public void successMethodInTransaction() {
		insert();
	}

	@Transactional // throws unchecked exception
	public void failMethodInTransaction() {
		insert();
		throw new RuntimeException("Deliberately throwing exception");
	}

	// transactional only from service wrapper, throws checked exception
	public void throwsException() throws Exception {
		insert();
		throw new Exception("Deliberately throwing exception");
	}

	public void insert() {
		session.query("CREATE (node {name: 'n'})", Collections.EMPTY_MAP);
	}

	public Iterable<Map<String, Object>> fetch() {
		return session.query("MATCH (n) RETURN n.name", new HashMap<String, Object>());
	}
}
