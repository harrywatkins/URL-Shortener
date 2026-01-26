package uk.co.tpximpact.urlshortener.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.co.tpximpact.urlshortener.controller.dto.ShortenRequest;
import uk.co.tpximpact.urlshortener.controller.dto.ShortenResponse;
import uk.co.tpximpact.urlshortener.controller.dto.UrlResponse;
import uk.co.tpximpact.urlshortener.service.UrlShortenerService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UrlShortenerControllerUnitTest {

    @Test
    void shorten_returns201AndBody_fromServiceResult() {
        UrlShortenerService service = mock(UrlShortenerService.class);
        UrlShortenerController controller = new UrlShortenerController(service);

        ShortenRequest req = new ShortenRequest();
        req.setFullUrl("https://example.com/very/long/url");
        req.setCustomAlias("my-custom-alias");

        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/shorten"));
        when(httpReq.getRequestURI()).thenReturn("/shorten");

        when(service.createShortUrl(eq("https://example.com/very/long/url"), eq("my-custom-alias"), anyString()))
                .thenReturn("http://localhost:8080/my-custom-alias");

        var resp = controller.shorten(req, httpReq);

        assertEquals(201, resp.getStatusCode().value());
        ShortenResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals("http://localhost:8080/my-custom-alias", body.shortUrl());

        // also verify the baseUrl passed into the service looks like a base url, not /shorten
        ArgumentCaptor<String> baseUrlCaptor = ArgumentCaptor.forClass(String.class);
        verify(service).createShortUrl(eq("https://example.com/very/long/url"), eq("my-custom-alias"), baseUrlCaptor.capture());
        assertFalse(baseUrlCaptor.getValue().endsWith("/shorten"));
    }

    @Test
    void redirect_returns302WithLocationHeader() {
        UrlShortenerService service = mock(UrlShortenerService.class);
        UrlShortenerController controller = new UrlShortenerController(service);

        when(service.resolveFullUrl("abc1234")).thenReturn("https://example.com/very/long/url");

        var resp = controller.redirect("abc1234");

        assertEquals(302, resp.getStatusCode().value());
        assertNotNull(resp.getHeaders().getLocation());
        assertEquals("https://example.com/very/long/url", resp.getHeaders().getLocation().toString());
        assertNull(resp.getBody());
    }

    @Test
    void delete_returns204() {
        UrlShortenerService service = mock(UrlShortenerService.class);
        UrlShortenerController controller = new UrlShortenerController(service);

        doNothing().when(service).delete("abc1234");

        var resp = controller.delete("abc1234");

        assertEquals(204, resp.getStatusCode().value());
        verify(service).delete("abc1234");
    }

    @Test
    void list_returns200AndMapsServiceRecords() {
        UrlShortenerService service = mock(UrlShortenerService.class);
        UrlShortenerController controller = new UrlShortenerController(service);

        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/urls"));
        when(httpReq.getRequestURI()).thenReturn("/urls");

        when(service.listAll(anyString())).thenReturn(List.of(
                new UrlShortenerService.UrlRecord(
                        "my-custom-alias",
                        "https://example.com/very/long/url",
                        "http://localhost:8080/my-custom-alias"
                )
        ));

        var resp = controller.list(httpReq);

        assertEquals(200, resp.getStatusCode().value());
        List<UrlResponse> body = resp.getBody();
        assertNotNull(body);
        assertEquals(1, body.size());
        assertEquals("my-custom-alias", body.getFirst().alias());
        assertEquals("https://example.com/very/long/url", body.getFirst().fullUrl());
        assertEquals("http://localhost:8080/my-custom-alias", body.getFirst().shortUrl());
    }

    @Test
    void shorten_whenCustomAliasMissing_passesNullToService() {
        UrlShortenerService service = mock(UrlShortenerService.class);
        UrlShortenerController controller = new UrlShortenerController(service);

        ShortenRequest req = new ShortenRequest();
        req.setFullUrl("https://example.com/very/long/url");
        req.setCustomAlias(null);

        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/shorten"));
        when(httpReq.getRequestURI()).thenReturn("/shorten");

        when(service.createShortUrl(eq("https://example.com/very/long/url"), isNull(), anyString()))
                .thenReturn("http://localhost:8080/abc1234");

        var resp = controller.shorten(req, httpReq);

        assertEquals(201, resp.getStatusCode().value());
        verify(service).createShortUrl(eq("https://example.com/very/long/url"), isNull(), anyString());
    }

    @Test
    void list_passesBaseUrlWithoutPathToService() {
        UrlShortenerService service = mock(UrlShortenerService.class);
        UrlShortenerController controller = new UrlShortenerController(service);

        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/urls"));
        when(httpReq.getRequestURI()).thenReturn("/urls");

        when(service.listAll(anyString())).thenReturn(List.of());

        controller.list(httpReq);

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(service).listAll(captor.capture());
        assertFalse(captor.getValue().endsWith("/urls"));
    }

    @Test
    void list_whenNoUrls_returnsEmptyArray() {
        UrlShortenerService service = mock(UrlShortenerService.class);
        UrlShortenerController controller = new UrlShortenerController(service);

        HttpServletRequest httpReq = mock(HttpServletRequest.class);
        when(httpReq.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/urls"));
        when(httpReq.getRequestURI()).thenReturn("/urls");

        when(service.listAll(anyString())).thenReturn(List.of());

        var resp = controller.list(httpReq);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());
    }
}
