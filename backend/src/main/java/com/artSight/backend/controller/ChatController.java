package com.artSight.backend.controller;

import com.artSight.backend.entity.Artwork;
import com.artSight.backend.repository.ArtworkRepository;
import com.artSight.backend.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/artworks")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ArtworkRepository artworkRepository;

    @PostMapping("/{id}/chat")
    public Map<String, String> chat(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String question = body.get("question");

        Optional<Artwork> artworkOpt = artworkRepository.findById(id);
        Map<String, String> response = new HashMap<>();

        if (artworkOpt.isEmpty()) {
            response.put("error", "Artwork not found");
            return response;
        }

        String answer = chatService.askAboutArtwork(artworkOpt.get(), question);
        response.put("answer", answer);
        return response;
    }
}