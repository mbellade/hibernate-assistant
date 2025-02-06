package org.hibernate.assistant;

import java.util.List;

import org.hibernate.assistant.domain.Address;
import org.hibernate.assistant.domain.Company;
import org.hibernate.assistant.domain.Employee;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hibernate.assistant.util.LanguageModels.testAssistant;

@SessionFactory
@DomainModel(annotatedClasses = { Company.class, Address.class, Employee.class })
public class SimpleTest {
	@Test
	public void testCompanyQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// create new HibernateAssistant with default model and memory settings
			final HibernateAssistant assistant = testAssistant( session.getMetamodel() );

			final String message = "Extract all companies with a name starting with any upper case vowel letter.";
			final List<Company> companies = assistant.createAiQuery( message, session, Company.class ).getResultList();

			System.out.println( "Companies : " + companies.size() );
			companies.forEach( company -> System.out.println( "Name: " + company.getName() ) );
		} );
	}

	@Test
	public void testAddressQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// create new HibernateAssistant with default model and memory settings
			final HibernateAssistant assistant = testAssistant( session.getMetamodel() );

			final String message = "Extract the address from companies. The address must have a street starting with 'Via'.";
			final List<Address> addresses = assistant.createAiQuery( message, session, Address.class ).getResultList();

			System.out.println( "Addresses : " + addresses.size() );
			addresses.forEach( address -> System.out.println( "Street: " + address.getStreet() ) );
		} );
	}

	@Test
	public void testQueryWithHQL(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// create new HibernateAssistant with default model and memory settings
			final HibernateAssistant assistant = testAssistant( session.getMetamodel() );

			final String message = "Extract the address from companies. The address must have a street starting with 'Via'.";
			final AiQuery<Address> aiQuery = assistant.createAiQuery( message, session, Address.class );


			final List<Address> addresses = session.createQuery( aiQuery.getHql(), Address.class ).getResultList();

			System.out.println( "Addresses : " + addresses.size() );
			addresses.forEach( address -> System.out.println( "Street: " + address.getStreet() ) );
		} );
	}

	@Test
	public void testEmployeeQueries(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final HibernateAssistant assistant = testAssistant( session.getMetamodel() );

			String message = "Extract all companies which have at least 2 employees whose first name starts with 'M'";
			final List<Company> companies = assistant.createAiQuery( message, session, Company.class ).getResultList();

			System.out.println( "Companies : " + companies.size() );
			companies.forEach( company -> System.out.println( "Name: " + company.getName() ) );

			message = "Extract all employees of companies in the city of 'Raleigh' whose salary is at least 100000.";
			final List<Employee> employees = assistant.createAiQuery( message, session, Employee.class ).getResultList();

			System.out.println( "Employees : " + employees.size() );
			employees.forEach( employee -> System.out.println( "First name: " + employee.getFirstName() ) );
		} );
	}

	@Test
	public void testNaturalLanguageFromQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// create new HibernateAssistant with default model and memory settings
			final HibernateAssistant assistant = testAssistant( session.getMetamodel() );

			final String message = "How many companies do not have an address?";
			final AiQuery<Company> aiQuery = assistant.createAiQuery( message, session, Company.class );

			final String naturalLanguageResult = assistant.executeQuery( aiQuery, session );

			System.out.println( "Result : " + naturalLanguageResult );
		} );
	}

	@Test
	public void testNaturalLanguageFromMessage(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		// create new HibernateAssistant with default model and memory settings
		final HibernateAssistant assistant = testAssistant( sessionFactory.getJpaMetamodel() );

		final String message = "How many company addresses have a street name that starts with the word 'Via'?";
		final String naturalLanguageResult = assistant.executeQuery( message, sessionFactory );

		// There is one company that does not have an address.
		System.out.println( "Result : " + naturalLanguageResult );
	}

	@BeforeAll
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Company( 1L, "Red Hat", new Address( "Milan", "Via Gustavo Fara" ) ) );
			session.persist( new Company( 2L, "IBM", new Address( "Segrate", "Circonvallazione Idroscalo" ) ) );
			session.persist( new Company( 3L, "Belladelli Giovanni", new Address( "Pegognaga", "Via Roma" ) ) );
			session.persist( new Company( 4L, "Another Company", null ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}
}
