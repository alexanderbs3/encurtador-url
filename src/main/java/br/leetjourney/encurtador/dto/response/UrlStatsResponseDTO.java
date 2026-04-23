package br.leetjourney.encurtador.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UrlStatsResponseDTO(
         String hash,

         String urlOriginal,

         long totalCliques,

         LocalDateTime dataCriacao,

         LocalDateTime dataExpiracao
) {
}
