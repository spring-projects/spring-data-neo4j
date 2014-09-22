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
package org.neo4j.rest.graphdb.index;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.neo4j.rest.graphdb.RequestResult;

import com.sun.jersey.api.client.ClientResponse;

/**
 * @author mh
 * @since 22.09.11
 */
public class RetrievedIndexInfo implements IndexInfo {
    private Map<String, ?> indexInfo;

    public RetrievedIndexInfo(RequestResult response) {
        if (response.statusIs(ClientResponse.Status.NO_CONTENT)) this.indexInfo = Collections.emptyMap();
        else this.indexInfo = (Map<String, ?>) response.toMap();
    }

    @Override
    public boolean checkConfig(String indexName, Map<String, String> config) {
        Map<String, String> existingConfig = (Map<String, String>) indexInfo.get(indexName);
        if (config == null) {
            return existingConfig != null;
        } else {
            if (existingConfig == null) {
                return false;
            } else {
                if (existingConfig.entrySet().containsAll(config.entrySet())) {
                    return true;
                } else {
                    throw new IllegalArgumentException("Index with the same name but different config exists!");
                }
            }
        }
    }

    @Override
    public String[] indexNames() {
        Set<String> keys = indexInfo.keySet();
        return keys.toArray(new String[keys.size()]);
    }

    @Override
    public boolean exists(String indexName) {
        return indexInfo.containsKey(indexName);
    }

    @Override
    public Map<String, String> getConfig(String name) {
        return (Map<String, String>) indexInfo.get(name);
    }
}
