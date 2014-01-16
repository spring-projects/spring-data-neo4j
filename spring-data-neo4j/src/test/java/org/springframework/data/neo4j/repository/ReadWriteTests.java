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
package org.springframework.data.neo4j.repository;

import org.junit.Ignore;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.impl.util.FileUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.neo4j.model.Car;
import org.springframework.data.neo4j.model.Volvo;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

@Ignore
public class ReadWriteTests {
    public static void main(String[] args) throws IOException {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:ReadWriteTests-context.xml");
        boolean delete = false;
        try {
            Neo4jTemplate template = ctx.getBean(Neo4jTemplate.class);
            Car car = findOne(template);
            if (car != null) {
                delete = true;
                assertEquals(Volvo.class, car.getClass());
            } else {
                Transaction tx = template.getGraphDatabase().beginTx();
                Volvo volvo = template.save(new Volvo());
                assertEquals(1, volvo.id.intValue());
                tx.success();
                tx.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            delete = true;
            throw new RuntimeException(e);
        } finally {
            ctx.close();
            if (delete) {
                FileUtils.deleteRecursively(new File("target/read-write.db"));
            }
        }
    }

    private static Car findOne(Neo4jTemplate template) {
        try {
            return template.findOne(1, Car.class);
        } catch (DataRetrievalFailureException nfe) {
            return null;
        }
    }
}
