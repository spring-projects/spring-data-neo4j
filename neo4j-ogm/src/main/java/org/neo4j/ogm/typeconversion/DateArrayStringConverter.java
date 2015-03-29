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
 * By default the OGM will map date arrays to UTC-based ISO8601 compliant
 * String arrays when being stored as a node / relationship property
 *
 * Users can override this behaviour for Date objects using
 * the appropriate annotations:
 *
 * @DateString("format") will convert between dates and strings
 * using a user defined date format, e.g. "yy-MM-dd"
 *
 * @DateLong will read and write dates as Long values in the database.
 *
 * @author Luanne Misquitta
 */
public class DateArrayStringConverter implements AttributeConverter<Date[], String[]> {

    private String format;

    public DateArrayStringConverter(String userDefinedFormat) {
        this.format = userDefinedFormat;
    }


    @Override
    public String[] toGraphProperty(Date[] value) {
        if (value == null) {
            return null;
        }
        String[] values = new String[(value.length)];

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        int i = 0;
        for (Date date : value) {
            values[i++] = simpleDateFormat.format(date);
        }
        return values;
    }

    @Override
    public Date[] toEntityAttribute(String[] dateValues) {
        if (dateValues == null) {
            return null;
        }
        Date[] dates = new Date[dateValues.length];

        int i = 0;
        try {
            for (String date : dateValues) {
                dates[i++] = new SimpleDateFormat(format).parse((String) date);
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return dates;

    }
}
