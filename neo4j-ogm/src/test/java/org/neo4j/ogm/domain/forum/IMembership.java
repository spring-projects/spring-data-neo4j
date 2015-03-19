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

import java.beans.Transient;

/**
 * @author Vince Bickers
 */
public interface IMembership {

    // we assert fees are stored as Integers in the DB.
    // at the moment we haven't decided what to do
    // about converting to complex number formats
    // or for that matter, dates.
    Integer getFees();

    @Transient boolean getCanPost();
    @Transient boolean getCanComment();
    @Transient boolean getCanFollow();
    @Transient IMembership[] getUpgrades();

}
