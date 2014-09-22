/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.rest.graphdb.query;

import org.neo4j.rest.graphdb.RequestResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author mh
 * @since 21.09.11
 */
public class CypherTxResult implements CypherResult {

    private final CypherTransaction.Result result;

    public CypherTxResult(CypherTransaction.Result result) {
        this.result = result;
    }

    public Collection<String> getColumns() {
        return result.getColumns();
    }

    public Iterable<List<Object>> getData() {
        return result.getRows();
    }

    @Override
    public Map asMap() {
        return map("columns",getColumns(),"data",getData());
    }
}
