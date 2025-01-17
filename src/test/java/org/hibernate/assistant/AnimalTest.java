package org.hibernate.assistant;

import java.util.List;

import org.hibernate.Session;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.animal.DomesticAnimal;
import org.hibernate.testing.orm.domain.animal.Human;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;

@Jpa(standardModels = StandardDomainModel.ANIMAL)
public class AnimalTest {
	@BeforeAll
	public void createData(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
		} );
	}

	@Test
	public void testHumanQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Session session = entityManager.unwrap( Session.class );
			// create new HibernateAssistant with default model and memory settings
			final HibernateAssistant assistant = HibernateAssistant.builder()
					.metamodel( session.getMetamodel() )
					.build();

			final String message = "Extract all humans that have at least two or more friends.";
			final List<Human> humans = assistant.createAiQuery( message, session, Human.class ).getResultList();
			System.out.println( "Humans : " + humans.size() );
		} );
	}

	@Test
	public void testSubsequentQueries(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Session session = entityManager.unwrap( Session.class );
			// create new HibernateAssistant with default model and memory settings
			final HibernateAssistant assistant = HibernateAssistant.builder()
					.metamodel( session.getMetamodel() )
					.chatMemory( MessageWindowChatMemory.withMaxMessages( 10 ) )
					.build();

			String message = "Return all domestic animals that do not have an owner.";
			final List<DomesticAnimal> unowned = assistant.createAiQuery( message, session, DomesticAnimal.class )
					.getResultList();

			System.out.println( "Unowned animals : " + unowned.size() );

			message = "Re-run the previous query, this time only for ones that do have an owner.";
			final List<DomesticAnimal> owned = assistant.createAiQuery( message, session, DomesticAnimal.class )
					.getResultList();

			System.out.println( "Owned animals : " + owned.size() );

			message = "Now run another query like the last one, with an additional restriction: the animal should have a body weight of at most 5.0.";
			final List<DomesticAnimal> ownedLight = assistant.createAiQuery( message, session, DomesticAnimal.class )
					.getResultList();

			System.out.println( "Owned light animals : " + ownedLight.size() );
		} );
	}
}
