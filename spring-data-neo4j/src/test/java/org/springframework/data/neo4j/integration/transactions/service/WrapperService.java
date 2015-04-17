/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */
package org.springframework.data.neo4j.integration.transactions.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * @author: Vince Bickers
 */
@Component
@Transactional
public class WrapperService {

    private static final Logger log = LoggerFactory.getLogger(BusinessService.class);

    @Autowired
    BusinessService businessService;

    public void composeSuccessThenFail() {
        businessService.successMethod();
        businessService.failMethod();
    }

    public void composeSuccessThenSuccess() {
        businessService.successMethod();
        businessService.successMethod();
    }

    public void composeForceThenFail() {
        businessService.forceMethod();
        businessService.failMethod();
    }

    public Iterable<Map<String, Object>> loadNodes() {
        return businessService.loadNodes();
    }

    public void purge() {
        businessService.purge();
    }
}
