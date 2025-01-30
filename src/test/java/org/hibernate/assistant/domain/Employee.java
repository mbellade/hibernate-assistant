package org.hibernate.assistant.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity(name = "Employee")
public class Employee {
	@Id
	private Long id;

	private String firstName;

	private String lastName;


	private float salary;

	@ManyToOne
	private Company company;

	public Employee() {
	}

	public Long getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public Company getCompany() {
		return company;
	}
}
