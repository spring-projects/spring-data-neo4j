package org.springframework.data.neo4j.types;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class PointerBuilderTest {
	
	@Test
	public void build2DPoint(){

		Coordinate coordinate = new Coordinate(12,56);

		CartesianPoint2d cartesianPoint2d = new CartesianPoint2d(coordinate);

		assertEquals(cartesianPoint2d, PointBuilder.withSrid(7203).build(coordinate));
	}

	@Test
	public void build3DPoint(){

		Coordinate coordinate = new Coordinate(12,56, 67.0);

		CartesianPoint3d cartesianPoint3d = new CartesianPoint3d(coordinate);

		assertEquals(cartesianPoint3d, PointBuilder.withSrid(9157).build(coordinate));
	}

	@Test
	public void build3DPointWithNewSRID(){

		Coordinate coordinate = new Coordinate(12,56, 67.0);

		GeographicPoint3d geographicPoint3d = new GeographicPoint3d(coordinate, 6454);

		assertEquals(geographicPoint3d, PointBuilder.withSrid(6454).build(coordinate));
	}

}
