package uk.co.tpximpact.urlshortener.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.co.tpximpact.urlshortener.controller.dto.ShortenRequest;
import uk.co.tpximpact.urlshortener.controller.dto.ShortenResponse;
import uk.co.tpximpact.urlshortener.controller.dto.UrlResponse;
import uk.co.tpximpact.urlshortener.service.UrlShortenerService;

import java.net.URI;
import java.util.List;

@RestController
public class UrlShortenerController {

    private final UrlShortenerService service;

    public UrlShortenerController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(
            @Valid @RequestBody ShortenRequest request,
            HttpServletRequest httpRequest
    ) {
        String baseUrl = ServletUriComponentsBuilder
                .fromRequestUri(httpRequest)
                .replacePath(null)
                .build()
                .toUriString();

        String shortUrl = service.createShortUrl(
                request.getFullUrl(),
                request.getCustomAlias(),
                baseUrl
        );

        return ResponseEntity
                .status(201)
                .body(new ShortenResponse(shortUrl));
    }

    @GetMapping("/{alias}")
    public ResponseEntity<Void> redirect(@PathVariable String alias) {
        String fullUrl = service.resolveFullUrl(alias);
        return ResponseEntity
                .status(302)
                .location(URI.create(fullUrl))
                .build();
    }

    @DeleteMapping("/{alias}")
    public ResponseEntity<Void> delete(@PathVariable String alias) {
        service.delete(alias);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/urls")
    public ResponseEntity<List<UrlResponse>> list(HttpServletRequest httpRequest) {
        String baseUrl = ServletUriComponentsBuilder
                .fromRequestUri(httpRequest)
                .replacePath(null)
                .build()
                .toUriString();

        List<UrlResponse> response = service.listAll(baseUrl).stream()
                .map(r -> new UrlResponse(r.alias(), r.fullUrl(), r.shortUrl()))
                .toList();

        return ResponseEntity.ok(response);
    }
}
