package br.com.nandesk.file.api;

import br.com.nandesk.file.application.FileService;
import br.com.nandesk.shared.security.CurrentUser;
import br.com.nandesk.shared.web.BusinessException;
import br.com.nandesk.shared.web.ErrorCode;
import br.com.nandesk.user.application.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Upload de <b>uma</b> foto por requisição, assim que tirada, com {@code Idempotency-Key} = {@code clientUploadId}
 * gerado no aparelho (RN-27, ADR-0014). Repetir a chave devolve o mesmo arquivo.
 */
@RestController
@RequestMapping("/api/v1/uploads")
@Tag(name = "Colaboração")
public class UploadController {

    private final FileService files;
    private final UserAccountService users;

    public UploadController(FileService files, UserAccountService users) {
        this.files = files;
        this.users = users;
    }

    @Operation(summary = "Enviar uma foto (RF-18, RN-06, RN-27)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<FileService.Upload> upload(
            @RequestHeader("Idempotency-Key") String idempotencyKey, @RequestPart("file") MultipartFile file)
            throws IOException {
        UUID clientUploadId;
        try {
            clientUploadId = UUID.fromString(idempotencyKey.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        var owner = users.require(CurrentUser.require().publicId());
        FileService.Upload upload = files.uploadTemp(owner, clientUploadId, file.getBytes());
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(upload);
    }
}
