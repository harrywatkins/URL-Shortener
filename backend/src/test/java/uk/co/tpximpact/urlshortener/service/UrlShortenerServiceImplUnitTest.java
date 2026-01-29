package uk.co.tpximpact.urlshortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import uk.co.tpximpact.urlshortener.repository.ShortUrlRepository;
import uk.co.tpximpact.urlshortener.repository.entity.ShortUrlEntity;
import uk.co.tpximpact.urlshortener.service.exception.BadRequestException;
import uk.co.tpximpact.urlshortener.service.exception.NotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UrlShortenerServiceImplUnitTest {

    private ShortUrlRepository repository;
    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ShortUrlRepository.class);
        service = new UrlShortenerServiceImpl(repository);
    }

    @Test
    void createShortUrl_withCustomAlias_returnsShortUrlUsingAlias() {
        when(repository.save(any(ShortUrlEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String shortUrl = service.createShortUrl(
                "https://example.com/very/long/url",
                "my-custom-alias",
                "http://localhost:8080"
        );

        assertEquals("http://localhost:8080/my-custom-alias", shortUrl);

        ArgumentCaptor<ShortUrlEntity> captor = ArgumentCaptor.forClass(ShortUrlEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("my-custom-alias", captor.getValue().getAlias());
        assertEquals("https://example.com/very/long/url", captor.getValue().getFullUrl());
    }

    @Test
    void createShortUrl_withCustomAlias_trimsWhitespace() {
        when(repository.save(any(ShortUrlEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String shortUrl = service.createShortUrl(
                "https://example.com/very/long/url",
                "  my-custom-alias  ",
                "http://localhost:8080"
        );

        assertEquals("http://localhost:8080/my-custom-alias", shortUrl);

        ArgumentCaptor<ShortUrlEntity> captor = ArgumentCaptor.forClass(ShortUrlEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("my-custom-alias", captor.getValue().getAlias());
    }

    @Test
    void createShortUrl_withoutCustomAlias_generatesRandomAlias_withExpectedFormat() {
        when(repository.save(any(ShortUrlEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0)); // allow save

        String shortUrl = service.createShortUrl(
                "https://example.com/very/long/url",
                null,
                "http://localhost:8080"
        );

        assertNotNull(shortUrl);
        assertTrue(shortUrl.startsWith("http://localhost:8080/"));

        String alias = shortUrl.substring("http://localhost:8080/".length());
        assertFalse(alias.isBlank());
        assertEquals(7, alias.length());
        assertTrue(alias.matches("^[a-zA-Z0-9]+$"), "Generated alias should be base62 (alnum only)");

        ArgumentCaptor<ShortUrlEntity> captor = ArgumentCaptor.forClass(ShortUrlEntity.class);
        verify(repository).save(captor.capture());
        assertEquals(alias, captor.getValue().getAlias());
        assertEquals("https://example.com/very/long/url", captor.getValue().getFullUrl());
    }

    @Test
    void createShortUrl_retriesWhenGeneratedAliasCollides_thenSucceeds() {
        // First save throws unique constraint violation, second save succeeds
        when(repository.save(any(ShortUrlEntity.class)))
                .thenThrow(new DataIntegrityViolationException("unique violation"))
                .thenAnswer(inv -> inv.getArgument(0));

        String shortUrl = service.createShortUrl(
                "https://example.com/very/long/url",
                null,
                "http://localhost:8080"
        );

        assertNotNull(shortUrl);
        assertTrue(shortUrl.startsWith("http://localhost:8080/"));

        verify(repository, times(2)).save(any(ShortUrlEntity.class));
    }

    @Test
    void createShortUrl_rejectsInvalidFullUrl() {
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.createShortUrl("not-a-url", null, "http://localhost:8080")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("fullurl"));
        verifyNoInteractions(repository);
    }

    @Test
    void createShortUrl_rejectsNonHttpScheme() {
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.createShortUrl("ftp://example.com/file", null, "http://localhost:8080")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("http"));
        verifyNoInteractions(repository);
    }

    @Test
    void createShortUrl_rejectsCustomAliasWithInvalidCharacters() {
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.createShortUrl("https://example.com", "bad alias!", "http://localhost:8080")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("customalias"));
        verifyNoInteractions(repository);
    }

    @Test
    void createShortUrl_rejectsCustomAliasTooShort() {
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.createShortUrl("https://example.com", "ab", "http://localhost:8080")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("customalias"));
        verifyNoInteractions(repository);
    }

    @Test
    void createShortUrl_rejectsCustomAliasTooLong() {
        String tooLong = "a".repeat(51);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.createShortUrl("https://example.com", tooLong, "http://localhost:8080")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("customalias"));
        verifyNoInteractions(repository);
    }

    @Test
    void createShortUrl_rejectsDuplicateCustomAlias_viaUniqueConstraint() {
        when(repository.save(any(ShortUrlEntity.class)))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.createShortUrl("https://example.com/1", "dupe-alias", "http://localhost:8080")
        );

        assertTrue(ex.getMessage().toLowerCase().contains("taken"));
        verify(repository).save(any(ShortUrlEntity.class));
    }

    @Test
    void resolveFullUrl_unknownAlias_throwsNotFound() {
        when(repository.findByAlias("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.resolveFullUrl("missing"));
        verify(repository).findByAlias("missing");
    }

    @Test
    void resolveFullUrl_knownAlias_returnsFullUrl() {
        when(repository.findByAlias("my-alias"))
                .thenReturn(Optional.of(new ShortUrlEntity("my-alias", "https://example.com/x")));

        String fullUrl = service.resolveFullUrl("my-alias");

        assertEquals("https://example.com/x", fullUrl);
    }

    @Test
    void delete_existingAlias_deletesIt() {
        when(repository.existsByAlias("todelete")).thenReturn(true);

        service.delete("todelete");

        verify(repository).existsByAlias("todelete");
        verify(repository).deleteByAlias("todelete");
    }

    @Test
    void delete_unknownAlias_throwsNotFound() {
        when(repository.existsByAlias("missing")).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.delete("missing"));

        verify(repository).existsByAlias("missing");
        verify(repository, never()).deleteByAlias(anyString());
    }

    @Test
    void listAll_returnsRecordsWithExpectedFields() {
        when(repository.findAll()).thenReturn(List.of(
                new ShortUrlEntity("alias-a", "https://example.com/a"),
                new ShortUrlEntity("alias-b", "https://example.com/b")
        ));

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
        // provide out-of-order entities, service should sort by alias
        when(repository.findAll()).thenReturn(List.of(
                new ShortUrlEntity("bbb", "https://example.com/2"),
                new ShortUrlEntity("aaa", "https://example.com/1")
        ));

        List<UrlShortenerService.UrlRecord> list = service.listAll("http://localhost:8080");

        assertEquals("aaa", list.get(0).alias());
        assertEquals("bbb", list.get(1).alias());
    }
}
