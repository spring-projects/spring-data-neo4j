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

package org.neo4j.ogm.domain.forum;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.domain.forum.activity.Activity;

import java.util.Date;
import java.util.List;

@NodeEntity(label ="User")
public class Member extends Login  {

    private IMembership memberShip;
    private Date renewalDate;
    @Relationship(type ="HAS_ACTIVITY")
    private Iterable<Activity> activityList;
    private List<Member> followers;
    private List<Member> followees;
    private Long membershipNumber;
    private int[] nicknames;

    public IMembership getMemberShip() {
        return memberShip;
    }

    public void setMemberShip(IMembership memberShip) {
        this.memberShip = memberShip;
    }

    public Date getRenewalDate() {
        return renewalDate;
    }

    public void setRenewalDate(Date renewalDate) {
        this.renewalDate = renewalDate;
    }

    @Relationship(type ="HAS_ACTIVITY")
    public Iterable<Activity> getActivityList() {
        return activityList;
    }

    @Relationship(type ="HAS_ACTIVITY")
    public void setActivityList(Iterable<Activity> activityList) {
        this.activityList = activityList;
    }

    public List<Member> getFollowers() {
        return followers;
    }

    public void setFollowers(List<Member> followers) {
        this.followers = followers;
    }

    public List<Member> getFollowees() {
        return followees;
    }

    public void setFollowees(List<Member> followees) {
        this.followees = followees;
    }

    public long getMembershipNumber() {
        return membershipNumber;
    }

    public void setMembershipNumber(long membershipNumber) {
        this.membershipNumber = membershipNumber;
    }

    public int[] getNicknames() {
        return nicknames;
    }

    public void setNicknames(int[] nicknames) {
        this.nicknames = nicknames;
    }

}
