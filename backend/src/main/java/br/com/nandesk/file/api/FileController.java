package br.com.nandesk.file.api;

import br.com.nandesk.file.application.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Entrega de imagens (9.9): públicas com cache imutável; evidências só para dono e moderadores, sem cache. */
@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "Arquivos")
public class FileController {

    private final FileService files;

    public FileController(FileService files) {
        this.files = files;
    }

    @Operation(summary = "Imagem (variant=thumb para a miniatura de 400 px)")
    @GetMapping("/{publicId}")
    ResponseEntity<InputStreamResource> file(
            @PathVariable UUID publicId, @RequestParam(required = false) String variant) {
        FileService.Content content = files.open(publicId, "thumb".equals(variant));
        CacheControl cache = content.publicCache()
                ? CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable()
                : CacheControl.noStore();
        return ResponseEntity.ok()
                .cacheControl(cache)
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(content.stream()));
    }
}
