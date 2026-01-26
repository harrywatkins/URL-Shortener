package uk.co.tpximpact.urlshortener.service;

import java.util.List;

public interface UrlShortenerService {

    String createShortUrl(String fullUrl, String customAlias, String baseUrl);

    String resolveFullUrl(String alias);

    void delete(String alias);

    List<UrlRecord> listAll(String baseUrl);

    record UrlRecord(String alias, String fullUrl, String shortUrl) {}
}
