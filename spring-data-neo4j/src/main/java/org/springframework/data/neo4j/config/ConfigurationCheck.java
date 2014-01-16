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

package org.springframework.data.neo4j.config;

import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

/**
 * Validates correct configuration of Neo4j and Spring, especially transaction-managers
 */
public class ConfigurationCheck implements ApplicationListener<ContextStartedEvent> {
    Neo4jTemplate template;
    PlatformTransactionManager transactionManager;

    public ConfigurationCheck(Neo4jTemplate template, PlatformTransactionManager transactionManager) {
        this.template = template;
        this.transactionManager = transactionManager;
    }

    @Override
    public void onApplicationEvent(ContextStartedEvent event) {
        checkConfiguration();
    }

    //@PostConstruct
    private void checkConfiguration() {
        checkInjection();
        checkSpringTransactionManager();
        checkNeo4jTransactionManager();
    }

    private void checkInjection() {
        if (template.getGraphDatabaseService()==null) {
            throw new BeanCreationException("graphDatabaseService not correctly configured, please refer to the manual, setup section");
        }
    }

    private void checkSpringTransactionManager() {
        try {
            TransactionStatus transaction = transactionManager.getTransaction(null);
            IteratorUtil.count(template.getGraphDatabaseService().getRelationshipTypes());
            transactionManager.commit(transaction);
        } catch(Exception e) {
            throw new BeanCreationException("transactionManager not correctly configured, please refer to the manual, setup section",e);
        }
    }

    private void checkNeo4jTransactionManager() {
        Transaction tx = null;
        try {
            tx = template.getGraphDatabase().beginTx();
            // read transaction
            IteratorUtil.count(template.getGraphDatabaseService().getRelationshipTypes());
            tx.success();
        } catch (Exception e) {
            if (tx != null) {
                tx.failure();
            }
            throw new BeanCreationException("transactionManager not correctly configured, please refer to the manual, setup section",e);
        } finally {
            try {
                if (tx != null) tx.close();
            } catch(Exception e) {
                // ignore
            }
        }
    }
}
