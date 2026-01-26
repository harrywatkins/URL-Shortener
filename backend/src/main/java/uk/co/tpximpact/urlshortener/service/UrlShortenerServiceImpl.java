package uk.co.tpximpact.urlshortener.service;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import uk.co.tpximpact.urlshortener.service.exception.BadRequestException;
import uk.co.tpximpact.urlshortener.service.exception.NotFoundException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    private final Map<String, String> store = new ConcurrentHashMap<>();

    @Override
    public String createShortUrl(String fullUrl, String customAlias, String baseUrl) {
        validateFullUrl(fullUrl);

        if (customAlias != null && !customAlias.isBlank()) {
            String alias = customAlias.trim();
            validateCustomAlias(alias);

            if (store.putIfAbsent(alias, fullUrl) != null) {
                throw new BadRequestException("Alias already taken");
            }

            return baseUrl + "/" + alias;
        }

        int attempts = 0;
        while (attempts++ < MAX_ALIAS_GENERATION_ATTEMPTS) {
            String alias = generateRandomAlias();
            if (store.putIfAbsent(alias, fullUrl) == null) {
                return baseUrl + "/" + alias;
            }
        }

        throw new IllegalStateException("Unable to generate unique alias after retries");
    }

    @Override
    public String resolveFullUrl(String alias) {
        String fullUrl = store.get(alias);
        if (fullUrl == null) {
            throw new NotFoundException("Alias not found");
        }
        return fullUrl;
    }

    @Override
    public void delete(String alias) {
        if (store.remove(alias) == null) {
            throw new NotFoundException("Alias not found");
        }
    }

    @Override
    public List<UrlRecord> listAll(String baseUrl) {
        List<UrlRecord> records = new ArrayList<>();
        store.forEach((alias, fullUrl) ->
                records.add(new UrlRecord(alias, fullUrl, baseUrl + "/" + alias))
        );
        records.sort(Comparator.comparing(UrlRecord::alias));
        return records;
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
