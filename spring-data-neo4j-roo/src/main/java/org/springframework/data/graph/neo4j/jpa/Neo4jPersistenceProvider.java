/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.graph.neo4j.jpa;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.index.IndexService;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.persistence.support.EntityInstantiator;

import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.LoadState;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;
import java.util.Map;

/**
 * @author Michael Hunger
 * @since 23.08.2010
 */
// todo handle additinal info + database path
@Configurable
public class Neo4jPersistenceProvider implements PersistenceProvider {
    @Resource
    private GraphDatabaseContext graphDatabaseContext;

    @Override
    public EntityManagerFactory createEntityManagerFactory(String emName, Map params) {
        System.out.println("emName = " + emName);
        System.out.println("params = " + params);
        return new Neo4jEntityManagerFactory(graphDatabaseContext, null,params);
    }

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map params) {
        System.out.println("info.getPersistenceProviderClassName() = " + info.getPersistenceProviderClassName());
        System.out.println("info.getManagedClassNames() = " + info.getManagedClassNames());
        System.out.println("info.getMappingFileNames() = " + info.getMappingFileNames());
        System.out.println("info.getTransactionType() = " + info.getTransactionType());
        System.out.println("info.getProperties() = " + info.getProperties());
        System.out.println("info.getPersistenceUnitName() = " + info.getPersistenceUnitName());
        System.out.println("params = " + params);
        return new Neo4jEntityManagerFactory(graphDatabaseContext,info,params);
    }

    @Override
    public ProviderUtil getProviderUtil() {
        return new ProviderUtil(){
            @Override
            public LoadState isLoadedWithoutReference(Object o, String s) {
                return LoadState.UNKNOWN;
            }

            @Override
            public LoadState isLoadedWithReference(Object o, String s) {
                return LoadState.UNKNOWN;
            }

            @Override
            public LoadState isLoaded(Object o) {
                return LoadState.UNKNOWN;
            }
        };
    }
}
