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

package org.springframework.data.neo4j.examples.jsr303.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.jsr303.domain.Adult;
import org.springframework.data.neo4j.examples.jsr303.repo.AdultRepository;
import org.springframework.stereotype.Service;

/**
 * @author Vince Bickers
 */
@Service
public class AdultService {

    @Autowired
    private AdultRepository repository;

    public Adult save(Adult adult) {
        return repository.save(adult);
    }
}
