package uk.co.tpximpact.urlshortener.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StubUrlShortenerService implements UrlShortenerService {

    /**
     * Temporary stub implementation.
     * Will be replaced by a real service with persistence (H2/JPA).
     */

    @Override
    public String createShortUrl(String fullUrl, String customAlias, String baseUrl) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public String resolveFullUrl(String alias) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void delete(String alias) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<UrlRecord> listAll(String baseUrl) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
