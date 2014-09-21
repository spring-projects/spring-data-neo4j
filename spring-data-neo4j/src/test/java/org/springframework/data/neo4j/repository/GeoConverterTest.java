package org.springframework.data.neo4j.repository;

import org.junit.Test;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;

import static org.junit.Assert.*;

public class GeoConverterTest {

    /*
       2|
        3
     ---|---4-
        |
        |1
     */
    public static final Point POINT_1 = new Point(1.23d, -4.56d);
    public static final Point POINT_2 = new Point(-1.23d, 4.56d);
    public static final Point POINT_3 = new Point(0d, 2d);
    public static final Point POINT_4 = new Point(4d, 0d);
    public static final Point POINT_5 = new Point(1.23d, 4.56d);
    public static final Point POINT_6 = new Point(-1.23d, -4.56d);

    @Test
    public void testToWktCoords() throws Exception {
        assertEquals("1.23 -4.56", GeoConverter.toWktCoords(POINT_1));
    }

    @Test
    public void testToWkt() throws Exception {
        assertEquals("POLYGON((1.23 -4.56,-1.23 4.56,0.0 2.0,1.23 -4.56))",GeoConverter.toWkt(new Polygon(POINT_1,POINT_2,POINT_3)));
    }

    @Test
    public void testToWellKnownText() throws Exception {
        assertEquals("POLYGON((1.23 -4.56,-1.23 4.56,0.0 2.0,1.23 -4.56))",GeoConverter.toWellKnownText(new Polygon(POINT_1,POINT_2,POINT_3)));
        assertEquals("POINT(1.23 -4.56)",GeoConverter.toWellKnownText(POINT_1));
        assertEquals("POINT(-1.23 4.56)",GeoConverter.toWellKnownText(POINT_2));
        assertEquals("POINT(0.0 2.0)",GeoConverter.toWellKnownText(POINT_3));

    }

    @Test
    public void testToPolygon() throws Exception {
        assertEquals(new Polygon(POINT_1,POINT_6,POINT_2, POINT_5),GeoConverter.toPolygon(new Box(POINT_1, POINT_2)));
    }

    @Test
    public void testFromWellKnownText() throws Exception {
        assertEquals(new Polygon(POINT_1,POINT_2,POINT_3),GeoConverter.fromWellKnownText("POLYGON((1.23 -4.56,-1.23 4.56,0.0 2.0,1.23 -4.56))"));
    }

    @Test
    public void testPointFromWellKnownText() throws Exception {
        assertEquals(POINT_1,GeoConverter.pointFromWellKnownText("POINT(1.23 -4.56)"));
        assertEquals(POINT_2,GeoConverter.pointFromWellKnownText("POINT(-1.23 4.56)"));
        assertEquals(POINT_3,GeoConverter.pointFromWellKnownText("POINT(0 2)"));
        assertEquals(POINT_4,GeoConverter.pointFromWellKnownText("POINT(4 0)"));
    }
}
