/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
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
