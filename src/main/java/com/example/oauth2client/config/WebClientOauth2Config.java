package com.example.oauth2client.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.UnAuthenticatedServerOAuth2AuthorizedClientRepository;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.function.Consumer;

@Configuration
@EnableConfigurationProperties(ExternalApiSettings.class)
public class WebClientOauth2Config {

  private static final String CLIENT_REGISTRATION_ID = "idam";

  private final ExternalApiSettings externalApiSettings;

  public WebClientOauth2Config(ExternalApiSettings externalApiSettings) {
    this.externalApiSettings = externalApiSettings;
  }

  /*@Bean
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
  }*/

  @Bean
  public WebClient webClientSecurityCustomizer(
          ReactiveClientRegistrationRepository clientRegistrations,
          ReactiveOAuth2AuthorizedClientService oAuth2AuthorizedClientService) {

    AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager manager =
            new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                    clientRegistrations, oAuth2AuthorizedClientService);

    /*AnonymousReactiveOAuth2AuthorizedClientManager manager =
            new AnonymousReactiveOAuth2AuthorizedClientManager(clientRegistrations,
                    new UnAuthenticatedServerOAuth2AuthorizedClientRepository());*/

    ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider =
            ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                    .authorizationCode()
                    .refreshToken()
                    .clientCredentials(clientCredentialsGrantBuilder ->
                            clientCredentialsGrantBuilder.accessTokenResponseClient(new IdamReactiveOAuth2AccessTokenResponseClient()))
                    .password()
                    .build();

    manager.setAuthorizedClientProvider(authorizedClientProvider);

    ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
            new ServerOAuth2AuthorizedClientExchangeFilterFunction(manager);

    oauth2.setDefaultClientRegistrationId(CLIENT_REGISTRATION_ID);

    return WebClient.builder()
            .baseUrl(externalApiSettings.getUri())
            .filter(oauth2)
            .build();
  }

  /**
   * Helper function to include the Spring CLIENT_REGISTRATION_ID_ATTR_NAME in a
   * properties Map
   *
   * @param provider - OAuth2 authorization provider name
   * @return consumer properties Map
   */
  public static Consumer<Map<String, Object>> getExchangeFilterWith(String provider) {
    return ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId(provider);
  }
}
