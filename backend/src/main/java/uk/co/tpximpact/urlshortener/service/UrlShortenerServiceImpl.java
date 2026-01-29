package uk.co.tpximpact.urlshortener.service;

import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.tpximpact.urlshortener.repository.ShortUrlRepository;
import uk.co.tpximpact.urlshortener.repository.entity.ShortUrlEntity;
import uk.co.tpximpact.urlshortener.service.exception.BadRequestException;
import uk.co.tpximpact.urlshortener.service.exception.NotFoundException;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Primary
public class UrlShortenerServiceImpl implements UrlShortenerService {

    private static final Pattern ALIAS_PATTERN =
            Pattern.compile("^[a-zA-Z0-9-]{3,50}$");

    private static final String BASE62 =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int GENERATED_ALIAS_LENGTH = 7;
    private static final int MAX_ALIAS_GENERATION_ATTEMPTS = 20;

    private final ShortUrlRepository repository;

    public UrlShortenerServiceImpl(ShortUrlRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public String createShortUrl(String fullUrl, String customAlias, String baseUrl) {
        validateFullUrl(fullUrl);

        if (customAlias != null && !customAlias.isBlank()) {
            String alias = customAlias.trim();
            validateCustomAlias(alias);

            try {
                repository.save(new ShortUrlEntity(alias, fullUrl));
            } catch (DataIntegrityViolationException ex) {
                throw new BadRequestException("Alias already taken");
            }

            return baseUrl + "/" + alias;
        }

        int attempts = 0;
        while (attempts++ < MAX_ALIAS_GENERATION_ATTEMPTS) {
            String alias = generateRandomAlias();
            try {
                repository.save(new ShortUrlEntity(alias, fullUrl));
                return baseUrl + "/" + alias;
            } catch (DataIntegrityViolationException ex) {
                // collision – retry
            }
        }

        throw new IllegalStateException("Unable to generate unique alias after retries");
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveFullUrl(String alias) {
        return repository.findByAlias(alias)
                .map(ShortUrlEntity::getFullUrl)
                .orElseThrow(() -> new NotFoundException("Alias not found"));
    }

    @Override
    @Transactional
    public void delete(String alias) {
        if (!repository.existsByAlias(alias)) {
            throw new NotFoundException("Alias not found");
        }
        repository.deleteByAlias(alias);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UrlRecord> listAll(String baseUrl) {
        return repository.findAll().stream()
                .map(e -> new UrlRecord(
                        e.getAlias(),
                        e.getFullUrl(),
                        baseUrl + "/" + e.getAlias()
                ))
                .sorted(Comparator.comparing(UrlRecord::alias))
                .toList();
    }

    /* ---------- validation ---------- */

    private void validateFullUrl(String fullUrl) {
        try {
            URI uri = URI.create(fullUrl);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new BadRequestException("fullUrl must start with http:// or https://");
            }
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("fullUrl is not a valid URL");
        }
    }

    private void validateCustomAlias(String alias) {
        if (!ALIAS_PATTERN.matcher(alias).matches()) {
            throw new BadRequestException(
                    "customAlias must match ^[a-zA-Z0-9-]{3,50}$"
            );
        }
    }

    /* ---------- alias generation ---------- */

    private String generateRandomAlias() {
        return ThreadLocalRandom.current()
                .ints(GENERATED_ALIAS_LENGTH, 0, BASE62.length())
                .mapToObj(BASE62::charAt)
                .map(String::valueOf)
                .collect(Collectors.joining());
    }
}
