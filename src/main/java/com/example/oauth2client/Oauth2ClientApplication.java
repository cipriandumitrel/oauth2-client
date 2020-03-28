package com.example.oauth2client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@SpringBootApplication
public class Oauth2ClientApplication {

  public static void main(String[] args) {
    SpringApplication.run(Oauth2ClientApplication.class, args);
  }
}

@Component
@Log4j2
@RequiredArgsConstructor
class Client {

  private final WebClient client;

  @EventListener(ApplicationReadyEvent.class)
  public void ready() {

    this.client
            .get()
            .uri(uriBuilder -> uriBuilder
                    .path("/api/v3/{tenant}/{entity}")
                    .queryParam("view", "flat")
                    .queryParam("version", "1")
                    .queryParam("state", "update")
                    .queryParam("fromSeq", "58000")
                    .queryParam("toSeq", "58100")
                    .build("nl", "contactchannels"))
            .retrieve()
            .bodyToFlux(String.class)
            .subscribe(entity -> log.info("Entity: " + entity));
  }

}

