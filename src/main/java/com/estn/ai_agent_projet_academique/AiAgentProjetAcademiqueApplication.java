package com.estn.ai_agent_projet_academique;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class AiAgentProjetAcademiqueApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplication.run(AiAgentProjetAcademiqueApplication.class, args);
    }

    // ── SimpleVectorStore (injecté dans DocumentUploadIndexor) ────
    @Bean
    @Primary
    public SimpleVectorStore simpleVectorStore(
            @Qualifier("mistralAiEmbeddingModel") EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    // ── VectorStore = même bean (pour AiAgent.similaritySearch) ──
    @Bean
    public org.springframework.ai.vectorstore.VectorStore vectorStore(
            SimpleVectorStore simpleVectorStore) {
        return simpleVectorStore;
    }

    // ── Mistral ChatClient ────────────────────────────────────────
    @Bean
    @Qualifier("mistralClient")
    public ChatClient mistralChatClient(MistralAiChatModel mistralModel) {
        return ChatClient.builder(mistralModel)
                .defaultSystem("""
                        Tu es un assistant intelligent et precis.
                        Tu reponds UNIQUEMENT en te basant sur le document fourni
                        ET sur l'historique de la conversation.
                        Si la reponse n'est pas dans le document, dis-le clairement.
                        Tu te souviens de tout ce qui a ete dit dans la conversation.
                        Reponds dans la meme langue que la question.
                        """)
                .build();
    }

    // ── CORS ──────────────────────────────────────────────────────
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

}


