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

package org.neo4j.ogm.typeconversion;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * By default the OGM will map date objects to UTC-based ISO8601 compliant
 * String values when being stored as a node / relationship property
 *
 * Users can override this behaviour for Date objects using
 * the appropriate annotations:
 *
 * @DateString("format") will convert between dates and strings
 * using a user defined date format, e.g. "yy-MM-dd"
 *
 * @DateLong will read and write dates as Long values in the database.
 *
 * @author Vince Bickers
 */
public class DateStringConverter implements AttributeConverter<Date, String> {

    private String format;

    public DateStringConverter(String userDefinedFormat) {
        this.format = userDefinedFormat;
    }

    @Override
    public String toGraphProperty(Date value) {
        if (value == null) return null;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat.format((Date) value);
    }

    @Override
    public Date toEntityAttribute(String value) {
        if (value == null) return null;
        try {
            return new SimpleDateFormat(format).parse((String) value);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

}
