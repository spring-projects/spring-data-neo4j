/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
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
import org.neo4j.ogm.model.GraphModel;
import org.neo4j.ogm.session.result.GraphRowModel;
import org.neo4j.shell.util.json.JSONArray;
import org.neo4j.shell.util.json.JSONException;
import org.neo4j.shell.util.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Neo4jResponse} that contains data in both graph and row formats.
 *
 * @author Luanne Misquitta
 */
public class GraphRowModelResponse implements Neo4jResponse<GraphRowModel> {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphRowModelResponse.class);

	private final ObjectMapper objectMapper;
	private final Neo4jResponse<String> response;

	public GraphRowModelResponse(Neo4jResponse<String> response, ObjectMapper objectMapper) {
		this.response = response;
		this.objectMapper = objectMapper;
		try {
			initialiseScan(ResponseRecord.RESULTS);
		} catch (Exception e) {
			//Ignore this exception since we're reading the JSON manually in next()
			//TODO look into enhancing the JSONResponse parsing
		}
	}

	@Override
	public GraphRowModel next() {
		String json = response.next();
		if (json != null) {
			try {
				GraphRowModel graphRowModel = new GraphRowModel();
				JSONObject jsonObject = getOuterObject(json);
				JSONArray dataObject = jsonObject.getJSONArray("results").getJSONObject(0).getJSONArray("data");
				for (int i = 0; i < dataObject.length(); i++) {
					String graphJson = dataObject.getJSONObject(i).getString("graph");
					String rowJson = dataObject.getJSONObject(i).getString("row");
					GraphModel graphModel = objectMapper.readValue(graphJson, GraphModel.class);
					Object[] rows = objectMapper.readValue(rowJson, Object[].class);
					graphRowModel.addGraphRowResult(graphModel, rows);
				}
				return graphRowModel;
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
	public void initialiseScan(ResponseRecord record) {
		response.initialiseScan(record);
	}

	@Override
	public String[] columns() {
		return response.columns();
	}

	@Override
	public int rowId() {
		return response.rowId();
	}

	private JSONObject getOuterObject(String json) throws JSONException {
		JSONObject outerObject;
		try {
			outerObject = new JSONObject(json);
		} catch (JSONException e) {
			outerObject = new JSONObject(json + "]}"); //TODO enhance the JSONParser to not strip off these 2 characters
		}
		return outerObject;
	}
}
