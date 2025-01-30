package org.hibernate.assistant.util;

import java.util.Locale;

import org.hibernate.assistant.HibernateAssistant;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.persistence.metamodel.Metamodel;

import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;

public class LanguageModels {
	public static final String OLLAMA_BASE_URL = "http://localhost:11434";
	public static final String GRANITE_31_8b = "granite3.1-dense:8b";
	public static final String GRANITE_CODE_20b = "granite-code:20b-instruct";
	public static final String GRANITE_CODE_8b = "granite-code:8b-instruct-128k-q4_1"; // used by recommended RH code assistant
	public static final String GRANITE_CODE_3b = "granite-code:3b";
	public static final String LLAMA_32 = "llama3.2";
	public static final String CODELLAMA = "codellama";
	public static final String CODELLAMA_13B_INSTRUCT = "codellama:13b-instruct";
	public static final String DEEPSEEK_R1_8B = "codellama:13b-instruct"; // distilled llama 8.03B
	public static final String DEEPSEEK_R1_14B = "codellama:13b-instruct"; // distilled qwen2 14.8B
	public static final String GPT_4O_MINI = "gpt-4o-mini"; // distilled qwen2 14.8B

	public static ChatLanguageModel testChatLanguageModel() {
		final String modelType = System.getProperty( "hibernate.assistant.model-type" );
		if ( modelType != null ) {
			final String baseUrl = System.getProperty( "hibernate.assistant.model-base-url" );
			final String modelName = System.getProperty( "hibernate.assistant.model-name" );
			final boolean structuredJson = Boolean.parseBoolean( System.getProperty( "hibernate.assistant.structured-json" ) );
			return switch ( modelType.toUpperCase( Locale.ROOT ) ) {
				case "OLLAMA" -> {
					final OllamaChatModel.OllamaChatModelBuilder builder = OllamaChatModel.builder()
							.baseUrl( baseUrl )
							.modelName( modelName )
							.temperature( 0.0 );
					if ( structuredJson ) {
						builder.supportedCapabilities( RESPONSE_FORMAT_JSON_SCHEMA );
					}
					yield builder.build();
				}
				case "OPENAI" -> {
					final OpenAiChatModel.OpenAiChatModelBuilder openAiChatModelBuilder = OpenAiChatModel.builder()
							.apiKey( System.getProperty( "hibernate.assistant.openai.api-key" ) )
							.modelName( modelName )
							.temperature( 0.0 );
					if ( structuredJson ) {
						openAiChatModelBuilder.responseFormat( "json_schema" ).strictJsonSchema( true );
					}
					yield openAiChatModelBuilder.build();
				}
				default -> throw new IllegalStateException( "Unsupported model type: " + modelType );
			};
		}

		// default configuration if no system properties are found
		return OllamaChatModel.builder()
				.baseUrl( OLLAMA_BASE_URL )
				.modelName( CODELLAMA )
				.supportedCapabilities( RESPONSE_FORMAT_JSON_SCHEMA )
				.temperature( 0.0 )
				.build();
	}

	public static HibernateAssistant testAssistant(Metamodel metamodel) {
		return testAssistant( metamodel, null );
	}

	public static HibernateAssistant testAssistant(Metamodel metamodel, ChatMemory memory) {
		return HibernateAssistant.builder()
				.chatModel( testChatLanguageModel() )
				.metamodel( metamodel )
				.chatMemory( memory )
				.build();
	}
}
