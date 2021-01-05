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
package org.springframework.data.neo4j.transaction.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.transaction.domain.User;
import org.springframework.data.neo4j.transaction.repo.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author vince
 */
@Component
public class ServiceA {

	@Autowired private UserRepository userRepository;

	@Autowired private ServiceB serviceB;

	@Transactional(rollbackFor = Exception.class)
	public void run() throws Exception {
		saveBilbo();
		serviceB.update();
	}

	public User saveBilbo() {
		return userRepository.save(new User("Bilbo baggins"));
	}
}
