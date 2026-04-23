package br.leetjourney.encurtador.service;


import br.leetjourney.encurtador.domain.Url;
import br.leetjourney.encurtador.dto.request.UrlRequestDTO;
import br.leetjourney.encurtador.dto.response.UrlResponseDTO;
import br.leetjourney.encurtador.dto.response.UrlStatsResponseDTO;
import br.leetjourney.encurtador.repository.UrlRepository;
import br.leetjourney.encurtador.util.Base62Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    private final UrlRepository urlRepository;


    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_ID_SEQ_KEY = "url_id_sequence";
    private static final String REDIS_URL_PREFIX = "url:";
    private static final String REDIS_CLICKS_PREFIX = "url_clicks:";


    public UrlResponseDTO encurtarUrl(UrlRequestDTO request, String baseUrl) {
        Long idSequence = redisTemplate.opsForValue().increment(REDIS_ID_SEQ_KEY);


        String hash = Base62Util.enconde(idSequence);

        Url novaUrl = Url.builder()
                .hashCurto(hash)
                .urlOriginal(request.urlOriginal())
                .dataExpiracao(request.dataExpiracao())
                .build();


        urlRepository.save(novaUrl);


        long tempoExpiracao = calcularTempoExpiracaoEmMinutos(request.dataExpiracao());
        if (tempoExpiracao > 0) {
            redisTemplate.opsForValue().set(REDIS_URL_PREFIX + hash, request.urlOriginal(),
                    Duration.ofMinutes(tempoExpiracao));
        }

        return UrlResponseDTO.builder()
                .urlOriginal(novaUrl.getUrlOriginal())
                .urlEncurtada(baseUrl + "/" + hash)
                .dataExpiracao(novaUrl.getDataExpiracao())
                .build();
    }

    public String buscarUrlOriginal(String hash) {
        // 1. Tenta buscar no Redis (Cache Hit)
        String urlEmCache = redisTemplate.opsForValue().get(REDIS_URL_PREFIX + hash);
        if (urlEmCache != null) {
            log.info("Cache HIT para o hash: {}", hash);
            // INCREMENTA O CLIQUE AQUI (Assíncrono na prática, pois é em memória)
            redisTemplate.opsForValue().increment(REDIS_CLICKS_PREFIX + hash);
            return urlEmCache;
        }

        // 2. Fallback: Se não tem no Redis (Cache Miss), busca no MySQL
        log.info("Cache MISS para o hash: {}. Buscando no MySQL.", hash);
        Url urlSalva = urlRepository.findByHashCurto(hash)
                .orElseThrow(() -> new RuntimeException("URL não encontrada")); // Trataremos isso melhor na próxima fase

        // Verifica se a URL já expirou no banco de dados
        if (urlSalva.getDataExpiracao() != null && urlSalva.getDataExpiracao().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("URL expirada");
        }

        // 3. Salva de volta no Redis para a próxima requisição
        redisTemplate.opsForValue().set(REDIS_URL_PREFIX + hash, urlSalva.getUrlOriginal(), Duration.ofDays(1));// Cache padrão de 1 dia
        redisTemplate.opsForValue().increment(REDIS_CLICKS_PREFIX + hash);

        return urlSalva.getUrlOriginal();
    }

    public UrlStatsResponseDTO obterEstatisticas(String hash) {
        // Busca a URL no banco de dados para pegar as datas
        Url urlSalva = urlRepository.findByHashCurto(hash)
                .orElseThrow(() -> new RuntimeException("URL não encontrada"));

        // Busca a contagem de cliques atualizada em tempo real no Redis
        String cliquesStr = redisTemplate.opsForValue().get(REDIS_CLICKS_PREFIX + hash);
        long totalCliques = cliquesStr != null ? Long.parseLong(cliquesStr) : 0L;

        return UrlStatsResponseDTO.builder()
                .hash(urlSalva.getHashCurto())
                .urlOriginal(urlSalva.getUrlOriginal())
                .totalCliques(totalCliques)
                .dataCriacao(urlSalva.getDataCriacao())
                .dataExpiracao(urlSalva.getDataExpiracao())
                .build();
    }

    private long calcularTempoExpiracaoEmMinutos(LocalDateTime dataExpiracao) {
        if (dataExpiracao == null) return 1440; // Default 24 horas se não informado
        return Duration.between(LocalDateTime.now(), dataExpiracao).toMinutes();
    }
}