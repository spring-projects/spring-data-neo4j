package org.springframework.data.neo4j.integration.jsr303.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.integration.jsr303.domain.Adult;
import org.springframework.data.neo4j.integration.jsr303.repo.AdultRepository;
import org.springframework.stereotype.Service;

@Service
public class AdultService {

    @Autowired
    private AdultRepository repository;

    public Adult save(Adult adult) {
        return repository.save(adult);
    }
}
