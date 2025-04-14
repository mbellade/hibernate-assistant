package org.hibernate.assistant.model;

import org.hibernate.SessionFactory;
import org.hibernate.assistant.domain.Address;
import org.hibernate.assistant.domain.Company;
import org.hibernate.assistant.domain.Employee;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;

import org.junit.jupiter.api.Test;

import static org.hibernate.assistant.internal.AssistantUtils.getDomainModelPrompt;

public class DomainModelTests {

	@Test
	public void testSimpleDomainModel() {
		final Metadata metadata = new MetadataSources().addAnnotatedClass( Address.class )
				.addAnnotatedClass( Company.class )
				.addAnnotatedClass( Employee.class )
				.buildMetadata();
		try (final SessionFactory sf = metadata.buildSessionFactory()) {
			final String result = getDomainModelPrompt( sf.getMetamodel(), '"' );
			System.out.println( result );

			// todo : create some meaningful assertions
		}
	}
}
