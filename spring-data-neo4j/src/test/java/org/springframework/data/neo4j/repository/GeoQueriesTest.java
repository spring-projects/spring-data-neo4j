package org.springframework.data.neo4j.repository;

import org.junit.Test;
import org.springframework.data.geo.*;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 31.03.14
 */
public class GeoQueriesTest {
    @Test
    public void testBoxToPolygon() throws Exception {
        Polygon polygon = GeoConverter.toPolygon(new Box(new Point(0, 0), new Point(100, 100)));
        assertEquals(asList(new Point(0, 0), new Point(100, 0), new Point(100, 100), new Point(0, 100)),polygon.getPoints());
    }

    @Test
    public void testCircleToPolygon() throws Exception {
        Polygon polygon = GeoConverter.toPolygon(new Circle(new Point(13, 52), new Distance(69, Metrics.MILES)), 4);
        List<Point> expected = asList(new Point(14.624269, 52), new Point(13, 53), new Point(11.375731, 52.000000), new Point(13.000000, 51.000000));
        List<Point> points = polygon.getPoints();
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).toString(),points.get(i).toString());
        }
    }

    @Test
    public void testPointToWkt() throws Exception {
        assertEquals("POINT(100.0 30.0)", GeoConverter.toWellKnownText(new Point(100, 30)));
    }
    @Test
    public void testBoxToWkt() throws Exception {
        assertEquals("POLYGON((30.0 30.0,100.0 30.0,100.0 100.0,30.0 100.0,30.0 30.0))", GeoConverter.toWellKnownText(new Box(new Point(30, 30), new Point(100,100))));
    }
    @Test
    public void textPolygonFromWkt() throws Exception {
        assertEquals(GeoConverter.toPolygon(new Box(new Point(30, 30), new Point(100,100))), GeoConverter.fromWellKnownText("POLYGON((30.0 30.0,100.0 30.0,100.0 100.0,30.0 100.0,30.0 30.0))"));
    }
    @Test
    public void testWktToPoint() throws Exception {
        assertEquals(new Point(100, 30.1), GeoConverter.pointFromWellKnownText("POINT (100 30.1 ) "));
    }
}
