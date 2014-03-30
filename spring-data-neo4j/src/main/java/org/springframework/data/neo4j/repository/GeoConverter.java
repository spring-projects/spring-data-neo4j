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

import org.springframework.core.convert.ConversionException;
import org.springframework.data.geo.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conversion Utility between Well-Known-Text and geospatial types
 */
public class GeoConverter {

    public static final Pattern WKT_POINT = Pattern.compile("^POINT *\\( *([\\d.]+) *([\\d.]+) *\\) *$",Pattern.CASE_INSENSITIVE);
    private static final String POINT = " *[\\d.]+ *[\\d.]+ *";
    public static final Pattern WKT_POLYGON = Pattern.compile("^POLYGON *\\( *\\(((?:" + POINT + ",)*" + POINT + ") *\\) *\\) *$", Pattern.CASE_INSENSITIVE);

    public static String toWktCoords(Point point) {
        return String.format(Locale.ENGLISH,"%s %s",point.getX(),point.getY());
    }

    public static String toWkt(Polygon shape) {
        StringBuilder wkt = new StringBuilder("POLYGON((");
        List<Point> points = shape.getPoints();
        if (!points.isEmpty()) {
            for (Point point : points) {
                wkt.append(toWktCoords(point)).append(",");
            }
            wkt.append(toWktCoords(points.get(0)));
        }
        wkt.append("))");
        return wkt.toString();
    }

    public static String toWellKnownText(Point point) throws ConversionException {
        return "POINT(" + toWktCoords(point) + ")";
    }

    public static String toWellKnownText(Shape shape) throws ConversionException {
        if (shape instanceof Point) return "POINT("+toWktCoords((Point)shape)+")";
        if (shape instanceof Polygon) return toWkt((Polygon) shape);
        if (shape instanceof Circle) return toWkt(toPolygon((Circle) shape, 12));
        if (shape instanceof Box) return toWkt(toPolygon((Box)shape));
        throw new RuntimeException("Could not convert shape to WKT " +shape);
    }

    // see: http://blog.fedecarg.com/2009/02/08/geo-proximity-search-the-haversine-equation/
    public static Polygon toPolygon(Circle circle, int segments) {
        float angle = 0;
        float delta = 2 * (float)Math.PI / segments;
        Point center = circle.getCenter();
        double milesPerDegree = 69;
        double verticalRadius = circle.getRadius().in(Metrics.MILES).getValue() / milesPerDegree;
        // modifier for latitude, see haversin
        double horizontalModifier = Math.abs(Math.cos(Math.toRadians(center.getY())));
        double horizontalRadius = verticalRadius / horizontalModifier;

        List<Point> points = new ArrayList<>(segments);
        for (int i=0;i<segments;i++) {
            points.add(new Point(
                    Math.cos(angle)*horizontalRadius+center.getX(),
                    Math.sin(angle)*verticalRadius+center.getY()));
            angle += delta;
        }
        return new Polygon(points);
    }

    public static Polygon toPolygon(Box box) {
        Point first = box.getFirst();
        Point second = box.getSecond();

        return new Polygon(
                new Point(first.getX(),first.getY()),
                new Point(second.getX(),first.getY()),
                new Point(second.getX(),second.getY()),
                new Point(first.getX(),second.getY()));
    }

    public static Polygon fromWellKnownText(String wkt) {
        Matcher matcher = WKT_POLYGON.matcher(wkt);
        if (matcher.matches()) {
            String[] pointStrings = matcher.group(1).split(" *, *");
            ArrayList<Point> points = new ArrayList<>(pointStrings.length);
            for (String pointString : pointStrings) {
                String[] coords = pointString.trim().split(" +");
                Point point = new Point(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]));
                if (points.contains(point)) continue;
                points.add(point);
            }
            return new Polygon(points);
        }
        throw new RuntimeException("Error parsing '"+wkt+"' as POINT(x y) well known text");
    }

    public static Point pointFromWellKnownText(String wkt) {
        Matcher matcher = WKT_POINT.matcher(wkt);
        if (matcher.matches()) {
            return new Point(Double.parseDouble(matcher.group(1)),Double.parseDouble(matcher.group(2)));
        }
        throw new RuntimeException("Error parsing '"+wkt+"' as POINT(x y) well known text");
    }
}
