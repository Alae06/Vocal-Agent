package com.estn.ai_agent_projet_academique.agent;

import com.estn.ai_agent_projet_academique.entities.ChatMessage;
import com.estn.ai_agent_projet_academique.rag.DocumentUploadIndexor;
import com.estn.ai_agent_projet_academique.repositories.ChatMessageRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiAgent {

    private final DocumentUploadIndexor documentUploadIndexor;
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ChatMessageRepository chatMessageRepository;

    private final Map<String, List<Message>> sessionHistories = new ConcurrentHashMap<>();

    public AiAgent(
            DocumentUploadIndexor documentUploadIndexor,
            @Qualifier("mistralClient") ChatClient chatClient,
            VectorStore vectorStore,
            ChatMessageRepository chatMessageRepository) {
        this.documentUploadIndexor = documentUploadIndexor;
        this.chatClient            = chatClient;
        this.vectorStore           = vectorStore;
        this.chatMessageRepository = chatMessageRepository;
    }

    public String uploadDocument(MultipartFile file) throws IOException {
        try {
            documentUploadIndexor.loadFile(file);
            return "Le fichier " + file.getOriginalFilename() + " a été indexé avec succès !";
        } catch (Exception e) {
            throw new IOException("Erreur lors de l'indexation : " + e.getMessage());
        }
    }

    public String ask(String question, String sessionId) {
        List<Message> history = sessionHistories
                .computeIfAbsent(sessionId, k -> new ArrayList<>());

        List<org.springframework.ai.document.Document> docs = vectorStore.similaritySearch(question);

        StringBuilder context = new StringBuilder();
        if (docs != null && !docs.isEmpty()) {
            context.append("CONTEXTE EXTRAIT DU DOCUMENT :\n");
            docs.forEach(doc -> context.append(doc.getText()).append("\n---\n"));
        }

        String systemInstructions = """
    RÔLE : Expert en analyse de documents. Ton unique but est de produire un texte Markdown STRICTEMENT STRUCTURE.
    
    REGLES D'OR DE FORMATAGE (OBLIGATOIRE) :
    1. Utilise '###' devant chaque nom de module.
    2. AVANT CHAQUE '###', insère DEUX SAUTS DE LIGNE (\\n\\n).
    3. Chaque action DOIT être sur sa propre ligne commençant par un tiret '-'.
    4. JAMAIS de gros paragraphes. Un titre, une liste d'actions, un titre, une liste.
    
    EXEMPLE STRICT :
    
    ### 📂 Gestion des Séances
    - **Créer une séance** : explication courte.
    - **Supprimer une séance** : explication courte.
    
    ### 📝 Gestion des Examens
    - **Ajouter un examen** : explication courte.
    
    INTERDICTION : Ne produis jamais de bloc de texte continu comme un paragraphe.
    """;

        List<Message> messagesToSend = new ArrayList<>();
        messagesToSend.add(new org.springframework.ai.chat.messages.SystemMessage(systemInstructions));

        messagesToSend.addAll(history);

        String promptWithContext = context + "\n\nQUESTION DE L'UTILISATEUR : " + question + 
                "\n\nN'OUBLIE PAS : Utilise des titres '### [Module]' sur de nouvelles lignes pour structurer ta réponse.";
        messagesToSend.add(new UserMessage(promptWithContext));

        String answer = chatClient
                .prompt(new Prompt(messagesToSend))
                .call()
                .content();

        System.out.println("DEBUG - Réponse brute de l'agent :\n" + answer);

        history.add(new UserMessage(question)); 
        history.add(new AssistantMessage(answer));

        chatMessageRepository.save(ChatMessage.builder()
                .question(question)
                .answer(answer)
                .model("mistral")
                .sessionId(sessionId)
                .createdAt(LocalDateTime.now())
                .build());

        return answer;
    }

    public List<ChatMessage> getHistory(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    public void clearHistory(String sessionId) {
        chatMessageRepository.deleteBySessionId(sessionId);
        sessionHistories.remove(sessionId);
    }
}