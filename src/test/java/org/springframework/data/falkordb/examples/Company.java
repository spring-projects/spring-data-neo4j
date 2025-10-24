/*
 * Copyright (c) 2023-2025 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.springframework.data.falkordb.examples;

import java.util.List;

import org.springframework.data.falkordb.core.schema.GeneratedValue;
import org.springframework.data.falkordb.core.schema.Id;
import org.springframework.data.falkordb.core.schema.Node;
import org.springframework.data.falkordb.core.schema.Property;
import org.springframework.data.falkordb.core.schema.Relationship;

/**
 * Example Company entity demonstrating FalkorDB node mapping.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@Node("Company")
public class Company {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	private String industry;

	private int foundedYear;

	@Property("employee_count")
	private int employeeCount;

	@Relationship(type = "EMPLOYS", direction = Relationship.Direction.INCOMING)
	private List<Person> employees;

	// Default constructor
	public Company() {
	}

	public Company(String name, String industry) {
		this.name = name;
		this.industry = industry;
	}

	// Getters and setters

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIndustry() {
		return this.industry;
	}

	public void setIndustry(String industry) {
		this.industry = industry;
	}

	public int getFoundedYear() {
		return this.foundedYear;
	}

	public void setFoundedYear(int foundedYear) {
		this.foundedYear = foundedYear;
	}

	public int getEmployeeCount() {
		return this.employeeCount;
	}

	public void setEmployeeCount(int employeeCount) {
		this.employeeCount = employeeCount;
	}

	public List<Person> getEmployees() {
		return this.employees;
	}

	public void setEmployees(List<Person> employees) {
		this.employees = employees;
	}

	@Override
	public String toString() {
		return "Company{" + "id=" + this.id + ", name='" + this.name + '\'' + ", industry='" + this.industry + '\''
				+ ", foundedYear=" + this.foundedYear + ", employeeCount=" + this.employeeCount + '}';
	}

}
