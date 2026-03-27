package com.company.common.attachment.web;

import com.company.common.attachment.core.AttachmentService;
import com.company.common.attachment.core.model.AttachmentDownloadResponse;
import com.company.common.attachment.core.model.AttachmentOwnerRef;
import com.company.common.attachment.core.model.AttachmentUploadRequest;
import com.company.common.attachment.core.model.AttachmentUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private static final java.util.regex.Pattern OWNER_TYPE_PATTERN =
            java.util.regex.Pattern.compile("^[A-Z][A-Z0-9_]{0,49}$");

    private final AttachmentService attachmentService;

    @PostMapping
    public ResponseEntity<AttachmentUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ownerType") String ownerType,
            @RequestParam("ownerId") Long ownerId,
            @RequestParam(value = "displayName", required = false) String displayName
    ) throws IOException {
        if (!OWNER_TYPE_PATTERN.matcher(ownerType).matches()) {
            return ResponseEntity.badRequest().build();
        }
        AttachmentUploadRequest request = new AttachmentUploadRequest(
                ownerType,
                ownerId,
                file.getOriginalFilename(),
                displayName,
                file.getInputStream(),
                file.getSize(),
                file.getContentType()
        );
        AttachmentUploadResponse response = attachmentService.upload(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) throws IOException {
        AttachmentDownloadResponse download = attachmentService.download(id);

        String encodedFilename = URLEncoder.encode(
                download.originalFilename(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.mimeType()))
                .contentLength(download.fileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFilename)
                .body(new InputStreamResource(download.inputStream()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        attachmentService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<AttachmentUploadResponse>> listByOwner(
            @RequestParam("ownerType") String ownerType,
            @RequestParam("ownerId") Long ownerId
    ) {
        AttachmentOwnerRef ownerRef = new AttachmentOwnerRef(ownerType, ownerId);
        List<AttachmentUploadResponse> list = attachmentService.findByOwner(ownerRef);
        return ResponseEntity.ok(list);
    }
}
