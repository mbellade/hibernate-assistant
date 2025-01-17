package org.example;

import java.util.List;

import org.hibernate.Session;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.example.domain.Address;
import org.example.domain.Company;

@Jpa(annotatedClasses = { Company.class, Address.class })
public class SimpleTest {
	@BeforeAll
	public void createData(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist( new Company( 1L, "Red Hat", new Address( "Milan", "Via Gustavo Fara" ) ) );
			entityManager.persist( new Company( 2L, "IBM", new Address( "Segrate", "Circonvallazione Idroscalo" ) ) );
			entityManager.persist( new Company( 3L, "Belladelli Giovanni", new Address( "Pegognaga", "Via Roma" ) ) );
			entityManager.persist( new Company( 4L, "Another Company", null ) );
		} );
	}

	@Test
	public void testCompanyQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Session session = entityManager.unwrap( Session.class );
			// create new HibernateAssistant with default model and memory settings
			final HibernateAssistant assistant = HibernateAssistant.builder()
					.metamodel( session.getMetamodel() )
					.build();

			final String message = "Extract all companies with a name starting with any upper case vowel letter.";
			final List<Company> companies = assistant.createAiQuery( message, session, Company.class ).getResultList();

			System.out.println( "Companies : " + companies.size() );
			companies.forEach( company -> System.out.println( "Name: " + company.getName() ) );
		} );
	}

	@Test
	public void testAddressQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Session session = entityManager.unwrap( Session.class );
			// create new HibernateAssistant with default model and memory settings
			final HibernateAssistant assistant = HibernateAssistant.builder()
					.metamodel( session.getMetamodel() )
					.build();

			final String message = "Extract the address from companies. The address must have a street starting with 'Via'.";
			final List<Address> addresses = assistant.createAiQuery( message, session, Address.class ).getResultList();

			System.out.println( "Addresses : " + addresses.size() );
			addresses.forEach( address -> System.out.println( "Street: " + address.getStreet() ) );
		} );
	}

	@Test
	public void testNaturalLanguageQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Session session = entityManager.unwrap( Session.class );
			// create new HibernateAssistant with default model and memory settings
			final HibernateAssistant assistant = HibernateAssistant.builder()
					.metamodel( session.getMetamodel() )
					.build();

			final String message = "How many companies do not have an address?";
			final AiQuery<Company> aiQuery = assistant.createAiQuery( message, session, Company.class );

			final String naturalLanguageResult = assistant.executeQuery( aiQuery, session );

			System.out.println( "Result : " + naturalLanguageResult );
		} );
	}
}
