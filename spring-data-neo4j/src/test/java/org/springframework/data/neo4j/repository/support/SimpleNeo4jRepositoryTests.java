/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.repository.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.cypher.query.SortOrder;
import org.neo4j.ogm.session.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public class SimpleNeo4jRepositoryTests {

	private final Session sessionMock = mock(Session.class);
	private final PageRequest oneElementPageRequest = PageRequest.of(0, 1);
	private final PageRequest twoElementPageRequest = PageRequest.of(0, 2);

	private SimpleNeo4jRepository<Object, Long> repository = new SimpleNeo4jRepository<>(Object.class, sessionMock);

	@Test
	public void pageCorrectForOneElementPageWithOneResult() {
		int expectedElementsOnPage = 1;
		int amountOfElementsInDatabase = 1;
		int expectedPageCount = 1;

		Page page = loadPage(oneElementPageRequest, amountOfElementsInDatabase);

		assertThat(page.getNumberOfElements()).isEqualTo(expectedElementsOnPage);
		assertThat(page.getTotalPages()).isEqualTo(expectedPageCount);
		assertThat(page.getTotalElements()).isEqualTo(amountOfElementsInDatabase);

		verify(sessionMock).countEntitiesOfType(eq(Object.class));
	}

	@Test
	public void pageCorrectForOneElementPageWithTwoResults() {
		int expectedElementsOnPage = 1;
		int amountOfElementsInDatabase = 2;
		int expectedPageCount = 2;

		Page page = loadPage(oneElementPageRequest, amountOfElementsInDatabase);

		assertThat(page.getNumberOfElements()).isEqualTo(expectedElementsOnPage);
		assertThat(page.getTotalPages()).isEqualTo(expectedPageCount);
		assertThat(page.getTotalElements()).isEqualTo(amountOfElementsInDatabase);

		verify(sessionMock).countEntitiesOfType(eq(Object.class));
	}

	@Test
	public void pageCorrectForMoreThanOneElementPageWithOneResult() {
		int expectedElementsOnPage = 1;
		int amountOfElementsInDatabase = 1;
		int expectedPageCount = 1;

		Page page = loadPage(twoElementPageRequest, amountOfElementsInDatabase);

		assertThat(page.getNumberOfElements()).isEqualTo(expectedElementsOnPage);
		assertThat(page.getTotalPages()).isEqualTo(expectedPageCount);
		assertThat(page.getTotalElements()).isEqualTo(amountOfElementsInDatabase);
	}

	@Test
	public void pageCorrectForMoreThanOneElementPageWithTwoResults() {
		int expectedElementsOnPage = 2;
		int amountOfElementsInDatabase = 2;
		int expectedPageCount = 1;

		Page page = loadPage(twoElementPageRequest, amountOfElementsInDatabase);

		assertThat(page.getNumberOfElements()).isEqualTo(expectedElementsOnPage);
		assertThat(page.getTotalPages()).isEqualTo(expectedPageCount);
		assertThat(page.getTotalElements()).isEqualTo(amountOfElementsInDatabase);

		verify(sessionMock).countEntitiesOfType(eq(Object.class));
	}

	@Test
	public void pageCorrectForSecondPage() {
		int expectedElementsOnPage = 1;
		int amountOfElementsInDatabase = 3;
		int expectedPageCount = 2;

		PageRequest twoElementPageRequestPageTwo = PageRequest.of(1, 2);
		Page page = loadPage(twoElementPageRequestPageTwo, amountOfElementsInDatabase);

		assertThat(page.getNumberOfElements()).isEqualTo(expectedElementsOnPage);
		assertThat(page.getTotalPages()).isEqualTo(expectedPageCount);
		assertThat(page.getTotalElements()).isEqualTo(amountOfElementsInDatabase);
	}

	@Test
	public void pageCorrectForEmptyPage() {
		int expectedElementsOnPage = 0;
		int amountOfElementsInDatabase = 3;
		int expectedPageCount = 2;

		PageRequest twoElementPageRequestPageThree = PageRequest.of(2, 2);
		Page page = loadPage(twoElementPageRequestPageThree, amountOfElementsInDatabase);

		assertThat(page.getNumberOfElements()).isEqualTo(expectedElementsOnPage);
		assertThat(page.getTotalPages()).isEqualTo(expectedPageCount);
		assertThat(page.getTotalElements()).isEqualTo(amountOfElementsInDatabase);
	}

	@Test
	public void pageCorrectForMiddlePageRequest() {
		int expectedElementsOnPage = 2;
		int amountOfElementsInDatabase = 5;
		int expectedPageCount = 3;

		PageRequest twoElementPageRequestPageThree = PageRequest.of(1, 2);
		Page page = loadPage(twoElementPageRequestPageThree, amountOfElementsInDatabase);

		assertThat(page.getNumberOfElements()).isEqualTo(expectedElementsOnPage);
		assertThat(page.getTotalPages()).isEqualTo(expectedPageCount);
		assertThat(page.getTotalElements()).isEqualTo(amountOfElementsInDatabase);
	}

	private Page loadPage(PageRequest requestedPage, long amountOfElementsInDatabase) {
		prepareSessionMock(requestedPage, amountOfElementsInDatabase);
		return repository.findAll(requestedPage);
	}

	private void prepareSessionMock(PageRequest requestedPage, long amountOfElementsInDatabase) {
		List<Object> databaseResult = new ArrayList<>();

		long countOfElementsOnCurrentPage = Math.min(requestedPage.getPageSize(),
				amountOfElementsInDatabase - (requestedPage.getPageSize() * requestedPage.getPageNumber()));

		for (int i = 0; i < countOfElementsOnCurrentPage; i++) {
			databaseResult.add("entry");
		}

		doReturn(databaseResult).when(sessionMock).loadAll(eq(Object.class), any(SortOrder.class), any(Pagination.class),
				anyInt());
		doReturn(amountOfElementsInDatabase).when(sessionMock).countEntitiesOfType(eq(Object.class));
	}

}
