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

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author: Vince Bickers
 */
@Component
public class WrapperService {

	@Autowired BusinessService businessService;

	@Transactional
	public void composeSuccessThenFail() {
		businessService.successMethodInTransaction();
		businessService.failMethodInTransaction();
	}

	@Transactional
	public void composeSuccessThenSuccess() {
		businessService.successMethodInTransaction();
		businessService.successMethodInTransaction();
	}

	@Transactional
	public void composeFailThenSuccess() {
		businessService.failMethodInTransaction();
		businessService.successMethodInTransaction();
	}

	@Transactional
	public void composeFailThenFail() {
		businessService.failMethodInTransaction();
		businessService.failMethodInTransaction();
	}

	@Transactional(rollbackFor = Exception.class)
	public void rollbackWithCheckedException() throws Exception {
		businessService.throwsException();
	}

	@Transactional(readOnly = true)
	public Iterable<Map<String, Object>> fetch() {
		return businessService.fetch();
	}
}
