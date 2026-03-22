package com.wipro.fraud.aiassistant.controller;

import com.wipro.fraud.aiassistant.dto.IngestionRequest;
import com.wipro.fraud.aiassistant.service.DocumentIngestionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class IngestionController {

    private final DocumentIngestionService ingestionService;

    public IngestionController(DocumentIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public String ingest(@RequestBody IngestionRequest request) {

        ingestionService.ingest(request.getText());

        return "Document stored successfully";
    }
}