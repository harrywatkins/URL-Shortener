package uk.co.tpximpact.urlshortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.tpximpact.urlshortener.service.exception.BadRequestException;
import uk.co.tpximpact.urlshortener.service.exception.NotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UrlShortenerServiceImplUnitTest {

    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        service = new UrlShortenerServiceImpl();
    }

    @Test
    void createShortUrl_withCustomAlias_returnsShortUrlUsingAlias() {
        String shortUrl = service.createShortUrl(
                "https://example.com/very/long/url",
                "my-custom-alias",
                "http://localhost:8080"
        );

        assertEquals("http://localhost:8080/my-custom-alias", shortUrl);
        assertEquals("https://example.com/very/long/url", service.resolveFullUrl("my-custom-alias"));
    }

    @Test
    void createShortUrl_withCustomAlias_trimsWhitespace() {
        String shortUrl = service.createShortUrl(
                "https://example.com/very/long/url",
                "  my-custom-alias  ",
                "http://localhost:8080"
        );

        assertEquals("http://localhost:8080/my-custom-alias", shortUrl);
        assertEquals("https://example.com/very/long/url", service.resolveFullUrl("my-custom-alias"));
    }

    @Test
    void createShortUrl_withoutCustomAlias_generatesRandomAlias_withExpectedFormat() {
        String shortUrl = service.createShortUrl(
                "https://example.com/very/long/url",
                null,
                "http://localhost:8080"
        );

        assertNotNull(shortUrl);
        assertTrue(shortUrl.startsWith("http://localhost:8080/"));

        String alias = shortUrl.substring("http://localhost:8080/".length());

        // Stronger assertions: not blank, expected length, expected allowed chars
        assertFalse(alias.isBlank());
        assertEquals(7, alias.length());
        assertTrue(alias.matches("^[a-zA-Z0-9]+$"), "Generated alias should be base62 (alnum only)");

        assertEquals("https://example.com/very/long/url", service.resolveFullUrl(alias));
    }

    @Test
    void createShortUrl_rejectsInvalidFullUrl() {
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.createShortUrl("not-a-url", null, "http://localhost:8080")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("fullurl"));
    }

    @Test
    void createShortUrl_rejectsNonHttpScheme() {
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.createShortUrl("ftp://example.com/file", null, "http://localhost:8080")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("http"));
    }

    @Test
    void createShortUrl_rejectsCustomAliasWithInvalidCharacters() {
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.createShortUrl("https://example.com", "bad alias!", "http://localhost:8080")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("customalias"));
    }

    @Test
    void createShortUrl_rejectsCustomAliasTooShort() {
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.createShortUrl("https://example.com", "ab", "http://localhost:8080")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("customalias"));
    }

    @Test
    void createShortUrl_rejectsCustomAliasTooLong() {
        String tooLong = "a".repeat(51);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.createShortUrl("https://example.com", tooLong, "http://localhost:8080")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("customalias"));
    }

    @Test
    void createShortUrl_rejectsDuplicateCustomAlias() {
        service.createShortUrl("https://example.com/1", "dupe-alias", "http://localhost:8080");

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.createShortUrl("https://example.com/2", "dupe-alias", "http://localhost:8080")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("taken"));
    }

    @Test
    void resolveFullUrl_unknownAlias_throwsNotFound() {
        assertThrows(NotFoundException.class, () -> service.resolveFullUrl("missing"));
    }

    @Test
    void delete_existingAlias_removesIt() {
        service.createShortUrl("https://example.com", "todelete", "http://localhost:8080");

        service.delete("todelete");

        assertThrows(NotFoundException.class, () -> service.resolveFullUrl("todelete"));
    }

    @Test
    void delete_unknownAlias_throwsNotFound() {
        assertThrows(NotFoundException.class, () -> service.delete("missing"));
    }

    @Test
    void listAll_returnsRecordsWithExpectedFields() {
        service.createShortUrl("https://example.com/a", "alias-a", "http://localhost:8080");
        service.createShortUrl("https://example.com/b", "alias-b", "http://localhost:8080");

        List<UrlShortenerService.UrlRecord> list = service.listAll("http://localhost:8080");

        assertEquals(2, list.size());

        assertTrue(list.stream().anyMatch(r ->
                r.alias().equals("alias-a")
                        && r.fullUrl().equals("https://example.com/a")
                        && r.shortUrl().equals("http://localhost:8080/alias-a")
        ));

        assertTrue(list.stream().anyMatch(r ->
                r.alias().equals("alias-b")
                        && r.fullUrl().equals("https://example.com/b")
                        && r.shortUrl().equals("http://localhost:8080/alias-b")
        ));
    }

    @Test
    void listAll_isSortedByAlias() {
        service.createShortUrl("https://example.com/2", "bbb", "http://localhost:8080");
        service.createShortUrl("https://example.com/1", "aaa", "http://localhost:8080");

        List<UrlShortenerService.UrlRecord> list = service.listAll("http://localhost:8080");

        assertEquals("aaa", list.get(0).alias());
        assertEquals("bbb", list.get(1).alias());
    }
}
