package br.com.nandesk.user.api;

import br.com.nandesk.shared.security.CurrentUser;
import br.com.nandesk.user.application.UserAccountService;
import br.com.nandesk.user.application.UserAccountService.ProfileView;
import br.com.nandesk.user.domain.CollaboratorProfile.PixKeyType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Meus dados e chave PIX (RF-11). A chave é cifrada e só volta mascarada. */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Conta")
public class MeController {

    private final UserAccountService users;

    public MeController(UserAccountService users) {
        this.users = users;
    }

    public record UpdateProfileRequest(
            @Size(max = 120) String name,

            @Size(max = 20) @Pattern(regexp = "^[0-9 ()+\\-]*$")
            String phone,

            @Pattern(regexp = "^(pt-BR|en|es)$") String locale,
            Boolean pushEnabled) {}

    public record PixKeyRequest(
            @NotNull PixKeyType type,
            @NotBlank @Size(max = 140) String key) {}

    @Operation(summary = "Meus dados (RF-11)")
    @GetMapping
    ResponseEntity<ProfileView> me() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(users.profile(CurrentUser.require().publicId()));
    }

    @Operation(summary = "Atualizar nome, telefone, idioma e push (RF-11)")
    @PatchMapping
    ResponseEntity<ProfileView> update(@Valid @RequestBody UpdateProfileRequest body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(users.updateProfile(
                        CurrentUser.require().publicId(),
                        body.name(),
                        body.phone(),
                        body.locale(),
                        body.pushEnabled()));
    }

    @Operation(summary = "Definir ou trocar a chave PIX (RF-11)")
    @PutMapping("/pix-key")
    ResponseEntity<ProfileView> pixKey(@Valid @RequestBody PixKeyRequest body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(users.setPixKey(CurrentUser.require().publicId(), body.type(), body.key()));
    }
}
