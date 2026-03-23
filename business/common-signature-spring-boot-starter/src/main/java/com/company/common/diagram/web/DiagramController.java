package com.company.common.diagram.web;

import com.company.common.diagram.dto.DiagramResponse;
import com.company.common.diagram.dto.DiagramSaveRequest;
import com.company.common.diagram.service.DiagramService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("${common.diagram.api-prefix:/api/diagrams}")
@RequiredArgsConstructor
public class DiagramController {

    private final DiagramService diagramService;

    @PostMapping
    public ResponseEntity<DiagramResponse> save(
            @RequestParam String diagramType,
            @RequestParam String ownerType,
            @RequestParam Long ownerId,
            @RequestParam(required = false) String name,
            @RequestParam String json,
            @RequestPart(required = false) MultipartFile image) throws IOException {

        DiagramSaveRequest request = new DiagramSaveRequest(diagramType, ownerType, ownerId, name, json);
        DiagramResponse response = diagramService.save(request, image);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<DiagramResponse> find(
            @RequestParam String diagramType,
            @RequestParam String ownerType,
            @RequestParam Long ownerId) {

        DiagramResponse response = diagramService.findByOwner(diagramType, ownerType, ownerId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<DiagramResponse>> history(
            @RequestParam String diagramType,
            @RequestParam String ownerType,
            @RequestParam Long ownerId) {

        List<DiagramResponse> history = diagramService.getHistory(diagramType, ownerType, ownerId);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @RequestParam String diagramType,
            @RequestParam String ownerType,
            @RequestParam Long ownerId) {

        diagramService.delete(diagramType, ownerType, ownerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<DiagramResponse> restore(@PathVariable Long id) {
        DiagramResponse response = diagramService.restoreVersion(id);
        return ResponseEntity.ok(response);
    }
}
