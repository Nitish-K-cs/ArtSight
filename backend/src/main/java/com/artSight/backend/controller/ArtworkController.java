package com.artSight.backend.controller;

import com.artSight.backend.dto.RecognitionResponse;
import com.artSight.backend.entity.Artwork;
import com.artSight.backend.repository.ArtworkRepository;
import com.artSight.backend.service.RecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/artworks")
public class ArtworkController {

    @Autowired
    private RecognitionService recognitionService;

    @Autowired
    private ArtworkRepository artworkRepository;

    @PostMapping("/recognize")
    public Map<String, Object> recognize(@RequestParam("file") MultipartFile file) throws Exception {
        RecognitionResponse recognition = recognitionService.recognize(file);

        Map<String, Object> response = new HashMap<>();

        if (recognition == null || !recognition.isMatched()) {
            response.put("matched", false);
            response.put("message", recognition != null ? recognition.getMessage() : "Recognition failed");
            return response;
        }

        Optional<Artwork> artworkOpt = artworkRepository.findById(recognition.getArtworkId());

        if (artworkOpt.isEmpty()) {
            response.put("matched", false);
            response.put("message", "Matched artwork not found in database");
            return response;
        }

        Artwork artwork = artworkOpt.get();
        response.put("matched", true);
        response.put("score", recognition.getScore());
        response.put("artwork", artwork);

        return response;
    }
}
