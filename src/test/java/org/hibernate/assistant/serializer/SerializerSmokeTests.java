package org.hibernate.assistant.serializer;

import org.hibernate.assistant.AiQuery;
import org.hibernate.assistant.domain.Address;
import org.hibernate.assistant.domain.Company;
import org.hibernate.assistant.domain.Employee;
import org.hibernate.assistant.internal.HibernateSerializer;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@DomainModel(annotatedClasses = {
		Address.class, Company.class, Employee.class,
})
@SessionFactory
public class SerializerSmokeTests {
	private HibernateSerializer serializer;

	private final ObjectMapper mapper = new ObjectMapper();

	@BeforeAll
	public void beforeAll(SessionFactoryScope scope) {
		serializer = new HibernateSerializer( scope.getSessionFactory() );
		scope.inTransaction( session -> {
			final Company rh = new Company( 1L, "Red Hat", new Address( "Milan", "Via Gustavo Fara" ) );
			session.persist( rh );
			final Company ibm = new Company( 2L, "IBM", new Address( "Segrate", "Circonvallazione Idroscalo" ) );
			session.persist( ibm );
			session.persist( new Company( 3L, "Belladelli Giovanni", new Address( "Pegognaga", "Via Roma" ) ) );
			session.persist( new Company( 4L, "Another Company", null ) );

			session.persist( new Employee( 1L, "Marco", "Belladelli", 100_000, rh ) );
			session.persist( new Employee( 2L, "Matteo", "Cauzzi", 50_000, rh ) );
			session.persist( new Employee( 3L, "Andrea", "Boriero", 200_000, ibm ) );
		} );
	}

	@Test
	public void testEmbeddedResult(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AiQuery<Address> q = AiQuery.from(
					"select address from Company where id = 1",
					Address.class,
					session
			);

			try {
				final String result = serializer.serializeToString( q.getResultList(), q );

				final JsonNode jsonNode = mapper.readTree( result );
				assertThat( jsonNode.get( "city" ).textValue() ).isEqualTo( "Milan" );
				assertThat( jsonNode.get( "street" ).textValue() ).isEqualTo( "Via Gustavo Fara" );
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testEmbeddedSubPartResult(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AiQuery<String> q = AiQuery.from(
					"select address.city from Company where id = 1",
					String.class,
					session
			);

			try {
				final String result = serializer.serializeToString( q.getResultList(), q );

				final JsonNode jsonNode = mapper.readTree( result );
				assertThat( jsonNode.textValue() ).isEqualTo( "Milan" );
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testNumericFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AiQuery<Long> q = AiQuery.from(
					"select max(id) from Company",
					Long.class,
					session
			);

			try {
				final String result = serializer.serializeToString( q.getResultList(), q );

				final JsonNode jsonNode = mapper.readTree( result );
				assertThat( jsonNode.intValue() ).isEqualTo( 4 );
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testCompany(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AiQuery<Company> q = AiQuery.from( "from Company where id = 1", Company.class, session );

			try {
				final String result = serializer.serializeToString( q.getResultList(), q );

				final JsonNode jsonNode = mapper.readTree( result );
				assertThat( jsonNode.get( "id" ).intValue() ).isEqualTo( 1 );
				assertThat( jsonNode.get( "name" ).textValue() ).isEqualTo( "Red Hat" );
				assertThat( jsonNode.get( "exployees" ).textValue() ).isEqualTo( "<uninitialized>" );

				final JsonNode address = jsonNode.get( "address" );
				assertThat( address.get( "city" ).textValue() ).isEqualTo( "Milan" );
				assertThat( address.get( "street" ).textValue() ).isEqualTo( "Via Gustavo Fara" );
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testCompanyFetchEmployees(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AiQuery<Company> q = AiQuery.from(
					"from Company c join fetch c.employees where c.id = 1",
					Company.class,
					session
			);

			try {
				final String result = serializer.serializeToString( q.getResultList(), q );

				final JsonNode jsonNode = mapper.readTree( result );
				assertThat( jsonNode.get( "id" ).intValue() ).isEqualTo( 1 );
				assertThat( jsonNode.get( "name" ).textValue() ).isEqualTo( "Red Hat" );

				final JsonNode employees = jsonNode.get( "employees" );
				assertThat( employees.isArray() ).isTrue();
				employees.forEach( employee -> {
					assertThat( employee.get( "id" ).intValue() ).isBetween( 1, 2 );
					assertThat( employee.get( "firstName" ).textValue() ).startsWith( "Ma" );
					assertThat( employee.get( "company" ).textValue() ).isEqualTo( Company.class.getName() + "#1" );
				} );
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}
}
