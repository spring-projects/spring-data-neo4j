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

import org.neo4j.ogm.session.result.ResultProcessingException;

import java.io.InputStream;
import java.util.Scanner;

/**
 * @author Vince Bickers
 */
public class JsonResponse implements Neo4jResponse<String> {

    private static final String COMMA = ",";
    private static final String START_RECORD_TOKEN = "{";
    private static final String NEXT_RECORD_TOKEN  = COMMA + START_RECORD_TOKEN;

    private static final String ERRORS_TOKEN = "],\"errors";
    private static final String COLUMNS_TOKEN = "{\"columns";

    private static final String GRAPH_TOKEN = "\"graph";
    private static final String ROW_TOKEN = "\"row";
    private static final String RESULTS_TOKEN = "\"results";
    private static final String STATS_TOKEN = "\"stats";


    private final InputStream results;
    private final Scanner scanner;
    private String scanToken = null;
    private String[] columns;
    private int currentRow = -1;

    public JsonResponse(InputStream results) {
        this.results = results;
        this.scanner = new Scanner(results, "UTF-8");
    }

    public void initialiseScan(ResponseRecord record) {
        this.scanToken = extractToken(record);
        this.scanner.useDelimiter(scanToken);
        // TODO: this currently assumes only ONE data[] element in the response stream.
        parseColumns();
    }

    public String next() {
        try {
            String json = scanner.next();

            while (!json.endsWith(NEXT_RECORD_TOKEN)) {
                // the scan token may be embedded in the current response record, we need to keep parsing...
                try {
                    String rest = scanner.next();
                    json = json + scanToken + rest;
                } catch (Exception e) {
                    break;
                }
            }

            // will match all records except last in response
            if (json.endsWith(NEXT_RECORD_TOKEN)) {
                json = json.substring(0, json.length() - NEXT_RECORD_TOKEN.length());
            } else if (json.contains(ERRORS_TOKEN)) {

                json = json.substring(0, json.indexOf(ERRORS_TOKEN));
                // todo: should check errors? they will usually not exist if we have data
            }
            String record = START_RECORD_TOKEN + scanToken + json;
            currentRow++;
            return record;

        } catch (Exception e) {
            return null;
        }
    }

    public void close() {
        try {
            results.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String[] columns() {
        return this.columns;
    }

    @Override
    public int rowId() {
        return currentRow;
    }

    private void parseColumns() {
        String header = this.scanner.next(); // consume the header and return the columns array to the caller

        int cp = header.indexOf(COLUMNS_TOKEN);
        if (cp == -1) {
            parseErrors(header);
        } else {
            String colStart = header.substring(cp);
            this.columns = colStart.substring(colStart.indexOf("[") + 1, colStart.indexOf("]")).replace("\"", "").split(",");
        }
    }

    private void parseErrors(String header) {
        int cp = header.indexOf(ERRORS_TOKEN);
        if (cp == -1) {
            throw new RuntimeException("Unexpected problem! Cypher response starts: " + header + "...");
        }

        StringBuilder sb = new StringBuilder(header);
        String response;
        try {
            while ((response = scanner.next()) != null) {
                sb.append(response);
            }
        } catch (Exception e) {
            scanner.close();
        }

        throw new ResultProcessingException(sb.substring(cp + 2), null);
    }

    private String extractToken(ResponseRecord format) {

        switch (format) {
            case GRAPH:
                return GRAPH_TOKEN;
            case ROW:
                return ROW_TOKEN;
            case RESULTS:
                return RESULTS_TOKEN;
            case STATS:
                return STATS_TOKEN;
            default:
                throw new RuntimeException("Unhandled response format: " + format);
        }
    }
}
