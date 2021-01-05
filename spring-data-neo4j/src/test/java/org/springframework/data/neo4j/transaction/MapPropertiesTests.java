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
package org.springframework.data.neo4j.transaction;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.neo4j.transaction.domain.ThreadInfo;
import org.springframework.data.neo4j.transaction.repo.ThreadInfoRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = MapPropertiesTests.ApplicationConfig.class)
@RunWith(SpringRunner.class)
public class MapPropertiesTests {

	@Autowired
	private ThreadInfoRepository threadInfoRepository;

	@Autowired
	private ThreadInfoService threadInfoService;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Test // DATAGRAPH-1247 and OGM GH-655
	public void mapPropertiesShouldBeDeletable() {

		for (Map<String, Long> clearedMap : new Map[] {
				Collections.<String, Long>emptyMap(),
				new HashMap<String, Long>(),
				null,
		}) {
			Long threadInfoId = transactionTemplate.execute(t -> {
				ThreadInfo threadInfo = new ThreadInfo();
				threadInfo.getServiceRelationship().put("a", 1L);
				threadInfoRepository.save(threadInfo);
				return threadInfo.getId();
			});

			transactionTemplate.executeWithoutResult((status) -> {
				ThreadInfo threadInfo = threadInfoRepository.findById(threadInfoId).get();
				assertThat(threadInfo.getServiceRelationship()).containsEntry("a", 1L);

				if (clearedMap == Collections.EMPTY_MAP) {
					threadInfo.getServiceRelationship().clear();
				} else {
					threadInfo.setServiceRelationship(clearedMap);
				}

				threadInfoRepository.save(threadInfo);
			});

			ThreadInfo threadInfo = threadInfoRepository.findById(threadInfoId).get();
			assertThat(threadInfo.getServiceRelationship()).isEmpty();
		}
	}

	@Test // DATAGRAPH-1247 and OGM GH-655
	public void mapPropertiesShouldBeDeletableInsideDeclarativeTransactions() {
		Long threadInfoId = transactionTemplate.execute(t -> {
			ThreadInfo threadInfo = new ThreadInfo();
			threadInfo.getServiceRelationship().put("a", 1L);
			threadInfoRepository.save(threadInfo);
			return threadInfo.getId();
		});

		threadInfoService.recommendedWayOfDoingComplexUpdates(threadInfoId);

		ThreadInfo threadInfo = threadInfoRepository.findById(threadInfoId).get();
		assertThat(threadInfo.getServiceRelationship()).isEmpty();
	}

	@Configuration
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.transaction.domain",
			repositoryPackages = "org.springframework.data.neo4j.transaction.repo")
	@EnableTransactionManagement
	static class ApplicationConfig {
		@Bean
		public ThreadInfoService threadInfoService(ThreadInfoRepository threadInfoRepository) {
			return new ThreadInfoService(threadInfoRepository);
		}
	}

	static class ThreadInfoService {
		private final ThreadInfoRepository threadInfoRepository;

		public ThreadInfoService(ThreadInfoRepository threadInfoRepository) {
			this.threadInfoRepository = threadInfoRepository;
		}

		@Transactional
		public void recommendedWayOfDoingComplexUpdates(Long id) {

			this.threadInfoRepository.findById(id).ifPresent(threadInfo -> {
				threadInfo.setMethod("The method");
				threadInfo.setServiceRelationship(Collections.emptyMap());

				threadInfoRepository.save(threadInfo);
			});
		}
	}
}
