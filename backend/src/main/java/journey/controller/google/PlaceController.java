package journey.controller.google;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/place")
public class PlaceController {

    @Autowired
    private final WebClient webClient;

    // private final String apiKey = "google api";

    public PlaceController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://maps.googleapis.com/maps/api/place/details/json").build();
    }

    @GetMapping("/details")
    public Mono<String> getPlaceDetails(@RequestParam String placeId,
            @RequestParam(defaultValue = "ja") String language) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("maps.googleapis.com")
                        .path("/maps/api/place/details/json")
                        .queryParam("place_id", placeId)
                        .queryParam("key", apiKey)
                        .queryParam("language", language)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }
}
