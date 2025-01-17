package org.hibernate.assistant;

import java.util.List;

import org.hibernate.Session;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.animal.DomesticAnimal;
import org.hibernate.testing.orm.domain.animal.Human;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@SessionFactory
@DomainModel(standardModels = StandardDomainModel.ANIMAL)
public class AnimalTest {
	@Test
	public void testHumanQuery(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Session session = entityManager.unwrap( Session.class );
			// create new HibernateAssistant with default model and memory settings
			final HibernateAssistant assistant = HibernateAssistant.builder()
					.metamodel( session.getMetamodel() )
					.build();

			final String message = "Extract all humans that have at least two or more friends.";
			final List<Human> humans = assistant.createAiQuery( message, session, Human.class ).getResultList();

			System.out.println( "Humans : " + humans.size() );
			humans.forEach( h -> System.out.println( "Human: " + h.getName() ) );
		} );
	}

	@Test
	public void testSubsequentQueries(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Session session = entityManager.unwrap( Session.class );
			// create new HibernateAssistant with default model and memory settings
			final HibernateAssistant assistant = HibernateAssistant.builder()
					.metamodel( session.getMetamodel() )
					.build();

			String message = "Return all domestic animals that do not have an owner.";
			final List<DomesticAnimal> unowned = assistant.createAiQuery( message, session, DomesticAnimal.class )
					.getResultList();

			System.out.println( "Unowned animals : " + unowned.size() );
			unowned.forEach( a -> System.out.println( "Animal: " + a.getDescription() ) );

			message = "Re-run the previous query, this time only for ones that do have an owner.";
			final List<DomesticAnimal> owned = assistant.createAiQuery( message, session, DomesticAnimal.class )
					.getResultList();

			System.out.println( "Owned animals : " + owned.size() );
			owned.forEach( a -> System.out.println( "Animal: " + a.getDescription() ) );

			message = "Now run another query like the last one, with an additional restriction: the animal should have a body weight of at most 5.0.";
			final List<DomesticAnimal> ownedLight = assistant.createAiQuery( message, session, DomesticAnimal.class )
					.getResultList();

			System.out.println( "Owned light animals : " + ownedLight.size() );
			ownedLight.forEach( a -> System.out.println( "Animal: " + a.getDescription() ) );
		} );
	}

	@BeforeAll
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}
}
