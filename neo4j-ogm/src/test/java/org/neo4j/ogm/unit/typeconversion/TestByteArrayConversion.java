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

package org.neo4j.ogm.unit.typeconversion;

import org.junit.Test;
import org.neo4j.ogm.domain.convertible.bytes.Photo;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.typeconversion.AttributeConverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestByteArrayConversion {

    private static final MetaData metaData = new MetaData("org.neo4j.ogm.domain.convertible.bytes");
    private static final ClassInfo photoInfo = metaData.classInfo("Photo");

    @Test
    public void testConvertersLoaded() {

        assertTrue(photoInfo.propertyField("image").hasConverter());
        assertTrue(photoInfo.propertyGetter("image").hasConverter());
        assertTrue(photoInfo.propertySetter("image").hasConverter());

    }

    @Test
    public void setImageAndCheck() {

        Photo photo = new Photo();
        AttributeConverter converter = photoInfo.propertyGetter("image").converter();

        photo.setImage(new byte[] {1,2,3,4});

        assertEquals("AQIDBA==", converter.toGraphProperty(photo.getImage()));
    }

    @Test
    public void getImageAndCheck() {

        Photo photo = new Photo();
        AttributeConverter converter = photoInfo.propertyGetter("image").converter();

        photo.setImage((byte[]) converter.toEntityAttribute("AQIDBA=="));

        byte[] image = photo.getImage();
        assertEquals(4, image.length);
        assertEquals(1, image[0]);
        assertEquals(2, image[1]);
        assertEquals(3, image[2]);
        assertEquals(4, image[3]);

    }


}
