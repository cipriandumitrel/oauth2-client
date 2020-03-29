package com.example.oauth2client;

import com.example.oauth2client.config.WebClientOauth2Config;

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

/*@Component
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

}*/

@Component
@Log4j2
@RequiredArgsConstructor
class Client {

  private final WebClient webClient;

  @EventListener(ApplicationReadyEvent.class)
  public void ready() {

    webClient
            .get()
            .uri(uriBuilder -> uriBuilder
                    .path("/api/v3/{tenant}/{entity}")
                    .queryParam("view", "flat")
                    .queryParam("version", "1")
                    .queryParam("state", "updated")
                    .queryParam("fromSeq", "70000")
                    .queryParam("toSeq", "72000")
                    .build("nl", "contactchannels"))
            //.attributes(WebClientOauth2Config.getExchangeFilterWith("idam"))
            .retrieve()
            .bodyToFlux(String.class)
            .doOnSubscribe(subscription -> log.info("Connection to security server"))
            .onErrorMap(err -> {
              try {
                // Wait if we receive an error
                Thread.sleep(100);
              } catch (InterruptedException e) {
                log.warn(e.toString());
              }
              return err;
            })
            .subscribe(str -> log.info("Server response : '{}'", str));
  }

}


