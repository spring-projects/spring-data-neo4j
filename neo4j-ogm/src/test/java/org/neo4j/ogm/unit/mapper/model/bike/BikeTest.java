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

package org.neo4j.ogm.unit.mapper.model.bike;

import org.junit.Test;
import org.neo4j.ogm.domain.bike.Bike;
import org.neo4j.ogm.domain.bike.Wheel;
import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.SessionFactory;

import java.util.Collection;

import static org.junit.Assert.*;

/**
 * @author Vince Bickers
 */
public class BikeTest {

    @Test
    public void testDeserialiseBikeModel() throws Exception {

        BikeRequest bikeRequest = new BikeRequest();

        SessionFactory sessionFactory = new SessionFactory("org.neo4j.ogm.domain.bike");
        Neo4jSession session = ((Neo4jSession) sessionFactory.openSession("dummy-url"));
        session.setRequest(bikeRequest);

        long now = -System.currentTimeMillis();
        Collection<Bike> bikes = session.loadAll(Bike.class);
        System.out.println("deserialised in " + (now + System.currentTimeMillis()) + " milliseconds");

        assertFalse(bikes.isEmpty());
        Bike bike = bikes.iterator().next();

        assertNotNull(bike);
        assertEquals(15, (long) bike.getId());
        assertEquals(2, bike.getColours().length);

        // check the frame
        assertEquals(18, (long) bike.getFrame().getId());
        assertEquals(27, (int) bike.getFrame().getSize());

        // check the saddle
        assertEquals(19, (long) bike.getSaddle().getId());
        assertEquals(42.99, bike.getSaddle().getPrice(), 0.00);
        assertEquals("plastic", bike.getSaddle().getMaterial());

        // check the wheels
        assertEquals(2, bike.getWheels().size());
        for (Wheel wheel : bike.getWheels()) {
            if (wheel.getId().equals(16L)) {
                assertEquals(3, (int) wheel.getSpokes());
            }
            if (wheel.getId().equals(17L)) {
                assertEquals(5, (int) wheel.getSpokes());
            }
        }
    }

    @Test
    public void testReloadExistingDomain() {

        BikeRequest bikeRequest = new BikeRequest();

        SessionFactory sessionFactory = new SessionFactory("org.neo4j.ogm.domain.bike");
        Neo4jSession session = ((Neo4jSession) sessionFactory.openSession("dummy-url"));
        session.setRequest(bikeRequest);

        Collection<Bike> bikes = session.loadAll(Bike.class);
        Collection<Bike> theSameBikes = session.loadAll(Bike.class);

        assertEquals(bikes.size(), theSameBikes.size());

    }

}
