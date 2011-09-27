package com.springone.myrestaurants.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Version;

@Entity
public class Restaurant {

    private String name;

    private String city;
    
    private String state;

    private String zipCode;

	public String getName() {
        return this.name;
    }

	public void setName(String name) {
        this.name = name;
    }

	public String getCity() {
        return this.city;
    }

	public void setCity(String city) {
        this.city = city;
    }

	public String getState() {
        return this.state;
    }

	public void setState(String state) {
        this.state = state;
    }

	public String getZipCode() {
        return this.zipCode;
    }

	public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

	@Version
    @Column(name = "version")
    private Integer version;

	public Long getId() {
        return this.id;
    }

	public void setId(Long id) {
        this.id = id;
    }

	public Integer getVersion() {
        return this.version;
    }

	public void setVersion(Integer version) {
        this.version = version;
    }

	public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Id: ").append(getId()).append(", ");
        sb.append("Version: ").append(getVersion()).append(", ");
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("City: ").append(getCity()).append(", ");
        sb.append("State: ").append(getState()).append(", ");
        sb.append("ZipCode: ").append(getZipCode());
        return sb.toString();
    }
}
