package org.springframework.data.neo4j.integration.jsr303.domain;

import javax.validation.constraints.Min;

public class Adult {

    private Long id;
    private String name;
    @Min(18)
    private Integer age;

    public Adult() {}

    public Adult(String name, Integer age) {
        this.name = name;
        this.age = age;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
