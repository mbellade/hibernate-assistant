package org.hibernate.assistant.serializer;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.assistant.AiQuery;
import org.hibernate.assistant.domain.Address;
import org.hibernate.assistant.domain.Company;
import org.hibernate.assistant.domain.Employee;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.animal.Cat;
import org.hibernate.testing.orm.domain.animal.Human;
import org.hibernate.testing.orm.domain.animal.Name;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.assistant.internal.HibernateSerializer.serializeToString;
import static org.junit.jupiter.api.Assertions.fail;

@DomainModel(annotatedClasses = {
		Address.class, Company.class, Employee.class,
}, standardModels = {
		StandardDomainModel.ANIMAL
})
@SessionFactory
public class SerializerSmokeTests {
	private final ObjectMapper mapper = new ObjectMapper();

	@BeforeAll
	public void beforeAll(SessionFactoryScope scope) {
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

			final Human human = human( 1L, session );
			cat( 2L, human, session );
		} );
	}

	@Test
	public void testEmbeddedResult(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AiQuery<Address> q = aiQuery(
					"select address from Company where id = 1",
					Address.class,
					session
			);

			try {
				final String result = serializeToString( q.getResultList(), q, scope.getSessionFactory() );

				final JsonNode jsonNode = getSingleValue( mapper.readTree( result ) );
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
			final AiQuery<String> q = aiQuery(
					"select address.city from Company where id = 1",
					String.class,
					session
			);

			try {
				final String result = serializeToString( q.getResultList(), q, scope.getSessionFactory() );

				final JsonNode jsonNode = getSingleValue( mapper.readTree( result ) );
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
			final AiQuery<Long> q = aiQuery(
					"select max(id) from Company",
					Long.class,
					session
			);

			try {
				final String result = serializeToString( q.getResultList(), q, scope.getSessionFactory() );

				final JsonNode jsonNode = getSingleValue( mapper.readTree( result ) );
				assertThat( jsonNode.intValue() ).isEqualTo( 4 );
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testStringyFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AiQuery<String> q = aiQuery(
					"select upper(name) from Company where id = 1",
					String.class,
					session
			);

			try {
				final String result = serializeToString( q.getResultList(), q, scope.getSessionFactory() );

				final JsonNode jsonNode = getSingleValue( mapper.readTree( result ) );
				assertThat( jsonNode.textValue() ).isEqualTo( "RED HAT" );
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testCompany(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AiQuery<Company> q = aiQuery( "from Company where id = 1", Company.class, session );

			try {
				final String result = serializeToString( q.getResultList(), q, scope.getSessionFactory() );

				final JsonNode jsonNode = getSingleValue( mapper.readTree( result ) );
				assertThat( jsonNode.get( "id" ).intValue() ).isEqualTo( 1 );
				assertThat( jsonNode.get( "name" ).textValue() ).isEqualTo( "Red Hat" );
				assertThat( jsonNode.get( "employees" ).textValue() ).isEqualTo( "<uninitialized>" );

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
			final AiQuery<Company> q = aiQuery(
					"from Company c join fetch c.employees where c.id = 1",
					Company.class,
					session
			);

			try {
				final String result = serializeToString( q.getResultList(), q, scope.getSessionFactory() );

				final JsonNode jsonNode = getSingleValue( mapper.readTree( result ) );
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

	@Test
	public void testSelectCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AiQuery<Employee> q = aiQuery(
					"select c.employees from Company c where c.id = 1",
					Employee.class,
					session
			);

			try {
				final String result = serializeToString( q.getResultList(), q, scope.getSessionFactory() );
				System.out.println(result);

				final JsonNode jsonNode = mapper.readTree( result );
				assertThat( jsonNode.isArray() ).isTrue();
				assertThat( jsonNode.size() ).isEqualTo( 2 );

				final JsonNode first = jsonNode.get( 0 );
				assertThat( first.isObject() ).isTrue();
				assertThat( first.get( "id" ).isIntegralNumber() ).isTrue();
				assertThat( first.get( "company" ).get( "name" ).textValue() ).isEqualTo( "Red Hat" );

				final JsonNode second = jsonNode.get( 1 );
				assertThat( second.isObject() ).isTrue();
				assertThat( second.get( "id" ).isIntegralNumber() ).isTrue();
				assertThat( second.get( "company" ).get( "name" ).textValue() ).isEqualTo( "Red Hat" );
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testSelectCollectionProperty(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AiQuery<String> q = aiQuery(
					"select element(c.employees).firstName from Company c where c.id = 1",
					String.class,
					session
			);

			try {
				final String result = serializeToString( q.getResultList(), q, scope.getSessionFactory() );
				System.out.println(result);

				final JsonNode jsonNode = mapper.readTree( result );
				assertThat( jsonNode.isArray() ).isTrue();
				assertThat( jsonNode.size() ).isEqualTo( 2 );
				assertThat( Set.of( jsonNode.get( 0 ).textValue(), jsonNode.get( 1 ).textValue() ) ).containsOnly(
						"Marco",
						"Matteo"
				);
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	@Test
	public void testComplexInheritance(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AiQuery<Human> q = aiQuery(
					"from Human h where h.id = 1",
					Human.class,
					session
			);

			try {
				final Human human = q.getSingleResult();

				Hibernate.initialize( human.getFamily() );
				assertThat( human.getFamily() ).hasSize( 1 );
				Hibernate.initialize( human.getPets() );
				assertThat( human.getPets() ).hasSize( 1 );
				Hibernate.initialize( human.getNickNames() );
				assertThat( human.getNickNames() ).hasSize( 2 );

				final String result = serializeToString( List.of( human ), q, scope.getSessionFactory() );

				final JsonNode jsonNode = getSingleValue( mapper.readTree( result ) );
				assertThat( jsonNode.get( "id" ).intValue() ).isEqualTo( 1 );

				final JsonNode family = jsonNode.get( "family" );
				assertThat( family.isObject() ).isTrue();
				assertThat( family.get( "sister" ).get( "description" ).textValue() ).isEqualTo( "Marco's sister" );

				final JsonNode pets = jsonNode.get( "pets" );
				assertThat( pets.isArray() ).isTrue();
				assertThat( pets.size() ).isEqualTo( 1 );
				final JsonNode cat = pets.get( 0 );
				assertThat( cat.isObject() ).isTrue();
				assertThat( cat.get( "id" ).intValue() ).isEqualTo( 2 );
				assertThat( cat.get( "description" ).textValue() ).isEqualTo( "Gatta" );
				assertThat( cat.get( "owner" ).isTextual() ).isTrue(); // circular relationship
				assertThat( cat.get( "owner" ).textValue() ).isEqualTo( Human.class.getName() + "#1" );

				final JsonNode nickNames = jsonNode.get( "nickNames" );
				assertThat( nickNames.isArray() ).isTrue();
				assertThat( nickNames.size() ).isEqualTo( 2 );
				assertThat( Set.of( nickNames.get( 0 ).textValue(), nickNames.get( 1 ).textValue() ) ).containsOnly(
						"Bella",
						"Eskimo Joe"
				);
			}
			catch (JsonProcessingException e) {
				fail( "Serialization failed with exception", e );
			}
		} );
	}

	private <T> AiQuery<T> aiQuery(String hql, Class<T> resultType, Session session) {
		return AiQuery.from( hql, resultType, null, session );
	}

	private JsonNode getSingleValue(JsonNode jsonNode) {
		assertThat( jsonNode.isArray() ).isTrue();
		assertThat( jsonNode.size() ).isEqualTo( 1 );
		return jsonNode.get( 0 );
	}

	private static Human human(Long id, Session session) {
		final Human human = new Human();
		human.setId( id );
		human.setName( new Name( "Marco", 'M', "Belladelli" ) );
		human.setBirthdate( new Date() );
		human.setNickNames( new TreeSet<>( Set.of( "Bella", "Eskimo Joe" ) ) );
		final Human sister = new Human();
		sister.setId( 99L );
		sister.setName( new Name( "Sister", 'S', "Belladelli" ) );
		sister.setDescription( "Marco's sister" );
		human.setFamily( Map.of( "sister", sister ) );
		session.persist( sister );
		session.persist( human );
		return human;
	}

	private static Cat cat(Long id, Human owner, Session session) {
		final Cat cat = new Cat();
		cat.setId( id );
		cat.setDescription( "Gatta" );
		cat.setOwner( owner );
		session.persist( cat );
		return cat;
	}
}
