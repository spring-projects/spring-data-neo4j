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

package org.neo4j.ogm.domain.convertible.date;

import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.annotation.typeconversion.DateLong;
import org.neo4j.ogm.annotation.typeconversion.DateString;

import java.util.Date;
import java.util.Set;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class Memo {

    private Long id;
    private String memo;

    // uses default ISO 8601 date format
    private Date recorded;

    // declares a custom converter
    @Convert(DateNumericStringConverter.class)
    private Date approved;

    @DateString("yyyy-MM-dd")
    private Date actioned;

    @DateLong
    private Date closed;

    // uses default ISO 8601 date format
    private Date[] escalations;

    // uses default ISO 8601 date format
    private Set<Date> implementations;

    public Memo() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Date getRecorded() {
        return recorded;
    }

    public void setRecorded(Date recorded) {
        this.recorded = recorded;
    }

    public Date getActioned() {
        return actioned;
    }

    @DateString("yyyy-MM-dd")
    public void setActioned(Date actioned) {
        this.actioned = actioned;
    }

    @DateLong
    public Date getClosed() {
        return closed;
    }

    public void setClosed(Date closed) {
        this.closed = closed;
    }

    public Date getApproved() {
        return approved;
    }

    @Convert(DateNumericStringConverter.class)
    public void setApproved(Date approved) {
        this.approved = approved;
    }

    public Date[] getEscalations() {
        return escalations;
    }

    public void setEscalations(Date[] escalations) {
        this.escalations = escalations;
    }

    public Set<Date> getImplementations() {
        return implementations;
    }

    public void setImplementations(Set<Date> implementations) {
        this.implementations = implementations;
    }
}
