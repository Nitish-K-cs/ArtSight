package com.artSight.backend.service;

import com.artSight.backend.entity.Artwork;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;

    @Autowired
    public ChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public String askAboutArtwork(Artwork artwork, String userQuestion) {
        String systemContext = String.format(
                "You are an art historian discussing the following artwork with a museum visitor. " +
                        "Title: %s. Artist: %s. Date: %s. Medium: %s. Culture: %s. " +
                        "Answer questions about this artwork accurately and engagingly, staying in context.",
                artwork.getTitle(), artwork.getArtist(), artwork.getDateCreated(),
                artwork.getMedium(), artwork.getCulture());

        return chatClient.prompt()
                .system(systemContext)
                .user(userQuestion)
                .call()
                .content();
    }
}