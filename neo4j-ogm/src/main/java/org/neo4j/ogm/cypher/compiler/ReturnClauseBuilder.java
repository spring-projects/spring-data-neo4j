/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.neo4j.ogm.cypher.compiler;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ReturnClauseBuilder implements CypherEmitter {
    @Override
    public boolean emit(StringBuilder queryBuilder, Map<String, Object> parameters, Set<String> varStack) {

        if (!varStack.isEmpty()) {
            queryBuilder.append(" RETURN ");
            for (Iterator<String> it = varStack.iterator(); it.hasNext(); ) {
                String var = it.next();
                queryBuilder.append("id(");
                queryBuilder.append(var);
                queryBuilder.append(") AS ");
                queryBuilder.append(var);
                if (it.hasNext()) {
                    queryBuilder.append(", ");
                }
            }
        }
        return !varStack.isEmpty();
    }
}
