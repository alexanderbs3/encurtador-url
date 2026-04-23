package br.leetjourney.encurtador.controller;


import br.leetjourney.encurtador.dto.request.UrlRequestDTO;
import br.leetjourney.encurtador.dto.response.UrlResponseDTO;
import br.leetjourney.encurtador.dto.response.UrlStatsResponseDTO;
import br.leetjourney.encurtador.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;


    @PostMapping("/api/v1/urls")
    public ResponseEntity<UrlResponseDTO> encurtarUrl(@Valid @RequestBody UrlRequestDTO request, HttpServletRequest httpRequest) {

        String baseUrl = httpRequest.getRequestURI()
                .toString().replace(httpRequest.getRequestURI(), "");

        UrlResponseDTO response = urlService.encurtarUrl(request, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/{hash}")
    public ResponseEntity<Void> redirecionar(@PathVariable String hash) {
        String urlOrginal = urlService.buscarUrlOriginal(hash);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(urlOrginal))
                .build();
    }

    @GetMapping("/api/v1/urls/{hash}/estatisticas")
    public ResponseEntity<UrlStatsResponseDTO> obterEstatisticas(@PathVariable String hash) {
        UrlStatsResponseDTO stats = urlService.obterEstatisticas(hash);
        return ResponseEntity.ok(stats);
    }
}
