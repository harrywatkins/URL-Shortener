package uk.co.tpximpact.urlshortener.repository.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "short_urls",
        uniqueConstraints = @UniqueConstraint(name = "uk_short_urls_alias", columnNames = "alias")
)
public class ShortUrlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String alias;

    @Column(nullable = false, length = 2048)
    private String fullUrl;

    protected ShortUrlEntity() {
        // JPA
    }

    public ShortUrlEntity(String alias, String fullUrl) {
        this.alias = alias;
        this.fullUrl = fullUrl;
    }

    public Long getId() { return id; }
    public String getAlias() { return alias; }
    public String getFullUrl() { return fullUrl; }
}
