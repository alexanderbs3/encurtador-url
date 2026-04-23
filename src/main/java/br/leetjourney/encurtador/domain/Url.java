package br.leetjourney.encurtador.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "urls")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hash_curto", unique = true, length = 10)
    private String hashCurto;


    @Column(name = "url_original", nullable = false, columnDefinition = "TEXT")
    private String urlOriginal;

    @CreationTimestamp
    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_expiracao")
    private LocalDateTime dataExpiracao;
}
