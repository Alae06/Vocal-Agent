package com.estn.ai_agent_projet_academique.controller;


import com.estn.ai_agent_projet_academique.agent.AiAgent;
import com.estn.ai_agent_projet_academique.entities.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AgentAIController {

    private final AiAgent aiAgent;

    // ── POST /api/upload ──────────────────────────────────────────
    @PostMapping("/docs/upload")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file) {
        try {
            String result = aiAgent.uploadDocument(file);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors du chargement : " + e.getMessage()));
        }
    }

    // ── POST /api/ask ─────────────────────────────────────────────
    @PostMapping("/chat/ask")
    public ResponseEntity<Map<String, String>> ask(
            @RequestBody Map<String, String> body) {

        String question  = body.getOrDefault("question", "").trim();
        String sessionId = body.getOrDefault("sessionId", "default");

        if (question.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La question ne peut pas etre vide"));
        }

        String answer = aiAgent.ask(question, sessionId);
        return ResponseEntity.ok(Map.of("answer", answer));
    }

    // ── GET /api/history/{sessionId} ──────────────────────────────
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessage>> history(
            @PathVariable String sessionId) {
        return ResponseEntity.ok(aiAgent.getHistory(sessionId));
    }

    // ── DELETE /api/history/{sessionId} ───────────────────────────
    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<Map<String, String>> clear(
            @PathVariable String sessionId) {
        aiAgent.clearHistory(sessionId);
        return ResponseEntity.ok(Map.of("message", "Conversation effacee"));
    }
}