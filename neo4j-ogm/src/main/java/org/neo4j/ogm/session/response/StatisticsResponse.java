/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */
package org.neo4j.ogm.session.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.ogm.session.result.QueryStatistics;
import org.neo4j.ogm.session.result.QueryStatisticsResult;
import org.neo4j.ogm.session.result.ResultProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Luanne Misquitta
 */
public class StatisticsResponse implements Neo4jResponse<QueryStatistics> {

	private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsResponse.class);

	private final ObjectMapper objectMapper;
	private final Neo4jResponse<String> response;

	public StatisticsResponse(Neo4jResponse<String> response, ObjectMapper mapper) {
		this.response = response;
		this.objectMapper = mapper;
		try {
			initialiseScan("stats");
		} catch (Exception e) {
			throw new ResultProcessingException("Could not initialise response", e);
		}
	}
	@Override
	public QueryStatistics next() {
		String json = response.next();

		if (json != null) {
			try {
				return objectMapper.readValue(json, QueryStatisticsResult.class).getStats();
			} catch (Exception e) {
				LOGGER.error("failed to parse: " + json);
				throw new RuntimeException(e);
			}
		} else {
			return null;
		}
	}

	@Override
	public void close() {
		response.close();
	}

	@Override
	public void initialiseScan(String token) {
		response.initialiseScan(token);
	}

	@Override
	public String[] columns() {
		return new String[0];
	}

	@Override
	public int rowId() {
		return -1;
	}
}
