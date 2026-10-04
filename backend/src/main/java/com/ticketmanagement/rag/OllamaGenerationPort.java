package com.ticketmanagement.rag;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

public class OllamaGenerationPort implements GenerationPort {

    private static final String SYSTEM = """
            You are a support assistant. Answer only from the ticket excerpts provided.
            Do not use general world knowledge. If the excerpts are insufficient, say so briefly.
            Do not invent ticket ids.
            """;

    private final ChatModel chatModel;

    public OllamaGenerationPort(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String generate(String question, List<VectorChunkStore.RetrievedChunk> context) {
        StringBuilder user = new StringBuilder();
        user.append("Question:\n").append(question).append("\n\nTicket excerpts:\n");
        for (VectorChunkStore.RetrievedChunk chunk : context) {
            user.append('[').append(chunk.ticketId()).append("]\n")
                    .append(chunk.content()).append("\n\n");
        }
        return chatModel.call(new Prompt(List.of(
                        new SystemMessage(SYSTEM),
                        new UserMessage(user.toString()))))
                .getResult()
                .getOutput()
                .getText();
    }
}
