package br.leetjourney.encurtador.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UrlResponseDTO(
        String urlOriginal,
        String urlEncurtada,
        LocalDateTime dataExpiracao
) {
}
