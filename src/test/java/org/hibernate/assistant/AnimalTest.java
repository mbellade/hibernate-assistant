package org.hibernate.assistant;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.animal.Cat;
import org.hibernate.testing.orm.domain.animal.Dog;
import org.hibernate.testing.orm.domain.animal.DomesticAnimal;
import org.hibernate.testing.orm.domain.animal.Human;
import org.hibernate.testing.orm.domain.animal.Name;
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
		scope.inTransaction( session -> {
			// create new HibernateAssistant with default model and memory settings
			final HibernateAssistant assistant = HibernateAssistant.builder()
					.metamodel( session.getMetamodel() )
					.build();

			final String message = "Extract all humans that have at least two or more friends.";
			final List<Human> humans = assistant.createAiQuery( message, session, Human.class ).getResultList();

			System.out.println( "Humans : " + humans.size() );
			humans.forEach( h -> System.out.println( "Human: " + h.getName().getFirst() + " " + h.getName()
					.getLast() ) );
		} );
	}

	@Test
	public void testSubsequentQueries(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// create new HibernateAssistant with default model and memory settings
			final HibernateAssistant assistant = HibernateAssistant.builder()
					.metamodel( session.getMetamodel() )
					.build();

			String message = "Return all domestic animals that do not have an owner.";
			final List<DomesticAnimal> unowned = assistant.createAiQuery( message, session, DomesticAnimal.class )
					.getResultList();

			System.out.println( "Unowned animals: " + unowned.size() );
			unowned.forEach( a -> System.out.println( "Animal: " + a.getDescription() ) );

			message = "Re-run the previous query, this time only for ones that do have an owner.";
			final List<DomesticAnimal> owned = assistant.createAiQuery( message, session, DomesticAnimal.class )
					.getResultList();

			System.out.println( "Owned animals: " + owned.size() );
			owned.forEach( a -> System.out.println( "Animal: " + a.getDescription() ) );

			message = "Now run another query exactly like the latest one, with an additional restriction: " +
					"the animal should have a body weight of at most 5.0.";
			final List<DomesticAnimal> ownedLight = assistant.createAiQuery( message, session, DomesticAnimal.class )
					.getResultList();

			System.out.println( "Owned light animals: " + ownedLight.size() );
			ownedLight.forEach( a -> System.out.println( "Animal: " + a.getDescription() ) );
		} );
	}

	@BeforeAll
	public void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// create some humans
			final Human marco = new Human();
			marco.setId( 1L );
			marco.setName( new Name( "Marco", 'm', "Belladelli" ) );
			session.persist( marco );
			final Human panda = new Human();
			panda.setId( 2L );
			panda.setName( new Name( "Marco", 'm', "Cauzzi" ) );
			session.persist( panda );
			final Human simo = new Human();
			simo.setId( 3L );
			simo.setName( new Name( "Stefano", 'm', "Simonazzi" ) );
			session.persist( simo );
			marco.setFriends( new ArrayList<>( List.of( simo, panda ) ) );

			// create some animals
			final Cat cat = new Cat();
			cat.setId( 4L );
			cat.setDescription( "gatta" );
			cat.setOwner( marco );
			cat.setBodyWeight( 3.4f );
			session.persist( cat );
			final Dog dog = new Dog();
			dog.setId( 5L );
			dog.setDescription( "leone" );
			dog.setOwner( panda );
			dog.setBodyWeight( 6.6f );
			session.persist( dog );
			final Cat stray = new Cat();
			stray.setId( 6L );
			stray.setDescription( "stray" );
			stray.setBodyWeight( 2.7f );
			session.persist( stray );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}
}
