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

package org.neo4j.ogm.integration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.domain.bike.Bike;
import org.neo4j.ogm.domain.bike.Frame;
import org.neo4j.ogm.domain.bike.Saddle;
import org.neo4j.ogm.domain.bike.Wheel;
import org.neo4j.ogm.session.SessionFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

/**
 * @author Michal Bachman
 */
public class EndToEndTest extends InMemoryServerTest {

    private static SessionFactory sessionFactory;

    @Before
    public void init() throws IOException {
        setUp();
        sessionFactory = new SessionFactory("org.neo4j.ogm.domain.bike");
        session = sessionFactory.openSession("http://localhost:" + neoPort);
    }

    @After
    public void tearDownTest() {
        tearDown();
    }

    @Test
    public void canSimpleQueryDatabase() {
        Saddle expected = new Saddle();
        expected.setPrice(29.95);
        expected.setMaterial("Leather");
        Wheel frontWheel = new Wheel();
        Wheel backWheel = new Wheel();
        Bike bike = new Bike();
        bike.setBrand("Huffy");
        bike.setWheels(Arrays.asList(frontWheel, backWheel));
        bike.setSaddle(expected);
        session.save(bike);

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("material", "Leather");
        Saddle actual = session.queryForObject(Saddle.class, "MATCH (saddle:Saddle{material: {material}}) RETURN saddle", parameters);

        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getMaterial(), actual.getMaterial());

        HashMap<String, Object> parameters2 = new HashMap<>();
        parameters2.put("brand", "Huffy");
        Bike actual2 = session.queryForObject(Bike.class, "MATCH (bike:Bike{brand: {brand}}) RETURN bike", parameters2);

        assertEquals(bike.getId(), actual2.getId());
        assertEquals(bike.getBrand(), actual2.getBrand());
        assertEquals(bike.getSaddle(), actual2.getSaddle());
        assertEquals(bike.getWheels().size(), actual2.getWheels().size());
    }


    @Test
    public void canSimpleScalarQueryDatabase() {
        Saddle expected = new Saddle();
        expected.setPrice(29.95);
        expected.setMaterial("Leather");
        session.save(expected);

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("material", "Leather");
        int actual = session.queryForObject(Integer.class, "MATCH (saddle:Saddle{material: {material}}) RETURN COUNT(saddle)", parameters);

        assertEquals(1, actual);
    }

    @Test
    public void canComplexQueryDatabase() {
        Saddle saddle = new Saddle();
        saddle.setPrice(29.95);
        saddle.setMaterial("Leather");
        Wheel frontWheel = new Wheel();
        Wheel backWheel = new Wheel();
        Bike bike = new Bike();
        bike.setBrand("Huffy");
        bike.setWheels(Arrays.asList(frontWheel, backWheel));
        bike.setSaddle(saddle);

        session.save(bike);

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("brand", "Huffy");
        Bike actual = session.queryForObject(Bike.class, "MATCH (bike:Bike{brand:{brand}})-[rels]-() RETURN bike, COLLECT(DISTINCT rels) as rels", parameters);

        assertEquals(bike.getId(), actual.getId());
        assertEquals(bike.getBrand(), actual.getBrand());
        assertEquals(bike.getWheels().size(), actual.getWheels().size());
        assertNotNull(actual.getSaddle());
    }

    @Test
    public void canComplexExecute() {
        Saddle saddle = new Saddle();
        saddle.setPrice(29.95);
        saddle.setMaterial("Leather");
        Wheel frontWheel = new Wheel();
        Wheel backWheel = new Wheel();
        Bike bike = new Bike();
        bike.setBrand("Huffy");
        bike.setWheels(Arrays.asList(frontWheel, backWheel));
        bike.setSaddle(saddle);

        session.save(bike);

        Saddle newSaddle = new Saddle();
        newSaddle.setPrice(19.95);
        newSaddle.setMaterial("Vinyl");

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("brand", "Huffy");
        parameters.put("saddle", newSaddle);
        session.execute("MATCH (bike:Bike{brand:{brand}})-[r]-(s:Saddle) SET s = {saddle}", parameters);

        HashMap<String, Object> parameters2 = new HashMap<>();
        parameters2.put("brand", "Huffy");
        Bike actual = session.queryForObject(Bike.class, "MATCH (bike:Bike{brand:{brand}})-[rels]-() RETURN bike, COLLECT(DISTINCT rels) as rels", parameters2);

        assertEquals(bike.getId(), actual.getId());
        assertEquals(bike.getBrand(), actual.getBrand());
        assertEquals(bike.getWheels().size(), actual.getWheels().size());
        assertEquals(actual.getSaddle().getMaterial(), "Vinyl");
    }

    @Test
    public void canSaveNewObjectTreeToDatabase() {

        Wheel frontWheel = new Wheel();
        Wheel backWheel = new Wheel();
        Bike bike = new Bike();

        bike.setFrame(new Frame());
        bike.setSaddle(new Saddle());
        bike.setWheels(Arrays.asList(frontWheel, backWheel));

        assertNull(frontWheel.getId());
        assertNull(backWheel.getId());
        assertNull(bike.getId());
        assertNull(bike.getFrame().getId());
        assertNull(bike.getSaddle().getId());

        session.save(bike);

        assertNotNull(frontWheel.getId());
        assertNotNull(backWheel.getId());
        assertNotNull(bike.getId());
        assertNotNull(bike.getFrame().getId());
        assertNotNull(bike.getSaddle().getId());

    }

    @Test
    public void tourDeFrance() {

        long now = -System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {

            Wheel frontWheel = new Wheel();
            Wheel backWheel = new Wheel();
            Bike bike = new Bike();

            bike.setFrame(new Frame());
            bike.setSaddle(new Saddle());
            bike.setWheels(Arrays.asList(frontWheel, backWheel));

            session.save(bike);
        }
        now += System.currentTimeMillis();

        System.out.println("Number of separate requests: 1000");
        System.out.println("Number of threads: 1");
        System.out.println("Number of new objects to create per request: 5");
        System.out.println("Number of relationships to create per request: 2");
        System.out.println("Average number of requests per second to HTTP TX endpoint (avg. throughput) : " + (int) (1000000.0 / now));
    }

    @Test
    public void multiThreadedTourDeFrance() throws InterruptedException {

        final int NUM_THREADS=4;
        final int NUM_INSERTS = 1000 / NUM_THREADS;

        List<Thread> threads = new ArrayList<>();

        long now = -System.currentTimeMillis();

        for (int i = 0; i < NUM_THREADS; i++) {
            Thread thread = new Thread( new Runnable() {

                @Override
                public void run() {
                    for (int i = 0; i < NUM_INSERTS; i++) {

                        Wheel frontWheel = new Wheel();
                        Wheel backWheel = new Wheel();
                        Bike bike = new Bike();

                        bike.setFrame(new Frame());
                        bike.setSaddle(new Saddle());
                        bike.setWheels(Arrays.asList(frontWheel, backWheel));

                        session.save(bike);
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }

        for (int i = 0; i < NUM_THREADS; i++) {
            threads.get(i).join();
        }
        now += System.currentTimeMillis();

        System.out.println("Number of separate requests: 1000");
        System.out.println("Number of threads: " + NUM_THREADS);
        System.out.println("Number of new objects to create per request: 5");
        System.out.println("Number of relationships to create per request: 2");
        System.out.println("Average number of requests per second to HTTP TX endpoint (avg. throughput) : " + (int) (1000000.0 / now));

    }

    @Test
    public void testRelationshipEntityHydration() {

    }
}
