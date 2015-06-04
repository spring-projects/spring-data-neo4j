
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

package org.neo4j.ogm.cypher.statement;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple parser for cypher queries to extract key-components of a cypher-query.
 * @author Rene Richter
 */
public class StatementParser {

    //I have chosen LinkedList to easily get first and last elements.
    public LinkedList<String> extractWithClauses(String statement) {

        String regex = "WITH.*(?=match)"; //Look for the word WITH and match everything until the word MATCH
        Pattern pattern = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
        return extractMatch(pattern,statement);

    }

    public LinkedList<String> extractAliases(String statement) {

        String regex =
                  "(?<=\\()\\w+(?=(:|\\)))"     //Match aliases for nodes
                + "|"                           //Alternatively
                + "(?<=\\[)\\w+(?=(:|\\]))"      //Match aliases for relationships
                + "|"                           //Alternatively
                + "(\\w+(?==))";               //Match aliases for paths.

        Pattern pattern = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
        return  extractMatch(pattern, statement);
    }

    //Helpermethod to collect matches.
    private LinkedList<String> extractMatch(Pattern pattern,String statement) {
        Matcher m = pattern.matcher(statement);

        LinkedList<String> returnValues = new LinkedList<>();

        while(m.find()) {
            returnValues.add(m.group());
        }
        return returnValues;
    }
}
