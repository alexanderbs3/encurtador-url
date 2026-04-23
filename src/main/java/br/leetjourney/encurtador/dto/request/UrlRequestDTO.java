package br.leetjourney.encurtador.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;


public record UrlRequestDTO(


        @NotBlank(message = "A URL original não pode estar vazia")
        @URL(message = "Formato de URL inválido")
        String urlOriginal,

        LocalDateTime dataExpiracao
) {}
