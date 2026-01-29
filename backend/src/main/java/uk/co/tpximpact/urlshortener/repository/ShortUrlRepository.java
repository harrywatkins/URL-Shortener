package uk.co.tpximpact.urlshortener.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.co.tpximpact.urlshortener.repository.entity.ShortUrlEntity;

import java.util.Optional;

public interface ShortUrlRepository extends JpaRepository<ShortUrlEntity, Long> {
    Optional<ShortUrlEntity> findByAlias(String alias);
    boolean existsByAlias(String alias);
    void deleteByAlias(String alias);
}
