/**
 * Copyright 2011 the original author or authors.
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

package org.springframework.data.graph.neo4j.config;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.impl.transaction.SpringTransactionManager;
import org.neo4j.kernel.impl.transaction.UserTransactionImpl;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.jta.UserTransactionAdapter;

/**
 * @author mh
 * @since 09.04.11
 */
public class TestTransactionManagerFactoryBean implements FactoryBean<JtaTransactionManager> {
    private GraphDatabaseService graphDatabaseService;
    private JtaTransactionManager transactionManager;

    @Override
    public JtaTransactionManager getObject() throws Exception {
        return transactionManager;
    }

    @Override
    public Class<?> getObjectType() {
        return JtaTransactionManager.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private JtaTransactionManager createJtaTransactionManager(GraphDatabaseService gds) {
        JtaTransactionManager jtaTm = new JtaTransactionManager();
        if (gds instanceof AbstractGraphDatabase) {
            jtaTm.setTransactionManager(new SpringTransactionManager(gds));
            jtaTm.setUserTransaction(new UserTransactionImpl(gds));
        } else {
            final NullTransactionManager tm = new NullTransactionManager();
            jtaTm.setTransactionManager(tm);
            jtaTm.setUserTransaction(new UserTransactionAdapter(tm));
        }
        return jtaTm;
    }

    public GraphDatabaseService getGraphDatabaseService() {
        return graphDatabaseService;
    }

    public void setGraphDatabaseService(GraphDatabaseService graphDatabaseService) {
        this.graphDatabaseService = graphDatabaseService;
        transactionManager = createJtaTransactionManager(graphDatabaseService);
    }

}
