package com.example.oauth2client.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.ClientCredentialsReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(ExternalApiSettings.class)
public class WebClientOauth2Config {

  private static final String CLIENT_REGISTRATION_ID = "idam";

  private final ExternalApiSettings externalApiSettings;

  public WebClientOauth2Config(ExternalApiSettings externalApiSettings) {
    this.externalApiSettings = externalApiSettings;
  }

  @Bean
  WebClient webClient(ReactiveClientRegistrationRepository reactiveClientRegistrationRepository,
                      ServerOAuth2AuthorizedClientRepository serverOAuth2AuthorizedClientRepository) {

    ClientCredentialsReactiveOAuth2AuthorizedClientProvider provider =
            new ClientCredentialsReactiveOAuth2AuthorizedClientProvider();
    provider.setAccessTokenResponseClient(new WebClientReactiveClientCredentialsTokenResponseClient());

    DefaultReactiveOAuth2AuthorizedClientManager manager =
            new DefaultReactiveOAuth2AuthorizedClientManager(reactiveClientRegistrationRepository,
                    serverOAuth2AuthorizedClientRepository);

    manager.setAuthorizedClientProvider(provider);

    ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
            new ServerOAuth2AuthorizedClientExchangeFilterFunction(manager);

    oauth2.setDefaultClientRegistrationId(CLIENT_REGISTRATION_ID);

    WebClient client = WebClient.builder()
            .baseUrl(externalApiSettings.getUri())
            .filter(oauth2)
            .build();

    return client;
    //return builder.baseUrl("http://localhost:8080")
    //            .build();


  }
}
