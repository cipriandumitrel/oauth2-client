package com.example.oauth2client.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.UnAuthenticatedServerOAuth2AuthorizedClientRepository;
import org.springframework.util.Assert;

import reactor.core.publisher.Mono;

public class AnonymousReactiveOAuth2AuthorizedClientManager implements ReactiveOAuth2AuthorizedClientManager {

  private final ReactiveClientRegistrationRepository clientRegistrationRepository;
  private final UnAuthenticatedServerOAuth2AuthorizedClientRepository authorizedClientRepository;
  private ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider;

  public AnonymousReactiveOAuth2AuthorizedClientManager(
          ReactiveClientRegistrationRepository clientRegistrationRepository,
          UnAuthenticatedServerOAuth2AuthorizedClientRepository authorizedClientRepository) {
    this.clientRegistrationRepository = clientRegistrationRepository;
    this.authorizedClientRepository = authorizedClientRepository;
  }

  @Override
  public Mono<OAuth2AuthorizedClient> authorize(OAuth2AuthorizeRequest authorizeRequest) {
    Assert.notNull(authorizeRequest, "authorizeRequest cannot be null");

    String clientRegistrationId = authorizeRequest.getClientRegistrationId();
    Authentication principal = authorizeRequest.getPrincipal();

    return Mono.justOrEmpty(authorizeRequest.getAuthorizedClient())
            .switchIfEmpty(Mono.defer(() -> this.authorizedClientRepository.loadAuthorizedClient(clientRegistrationId, principal, null)))
            .flatMap(authorizedClient -> {
              // Re-authorize
              return Mono.just(OAuth2AuthorizationContext.withAuthorizedClient(authorizedClient).principal(principal).build())
                      .flatMap(this.authorizedClientProvider::authorize)
                      .flatMap(reauthorizedClient -> this.authorizedClientRepository.saveAuthorizedClient(reauthorizedClient, principal, null).thenReturn(reauthorizedClient))
                      // Default to the existing authorizedClient if the client was not re-authorized
                      .defaultIfEmpty(authorizeRequest.getAuthorizedClient() != null ?
                              authorizeRequest.getAuthorizedClient() : authorizedClient);
            })
            .switchIfEmpty(Mono.deferWithContext(context ->
                    // Authorize
                    this.clientRegistrationRepository.findByRegistrationId(clientRegistrationId)
                            .switchIfEmpty(Mono.error(() -> new IllegalArgumentException(
                                    "Could not find ClientRegistration with id '" + clientRegistrationId + "'")))
                            .flatMap(clientRegistration -> Mono.just(OAuth2AuthorizationContext.withClientRegistration(clientRegistration).principal(principal).build()))
                            .flatMap(this.authorizedClientProvider::authorize)
                            .flatMap(authorizedClient -> this.authorizedClientRepository.saveAuthorizedClient(authorizedClient, principal, null).thenReturn(authorizedClient))
                            .subscriberContext(context)
            ));
  }

  public void setAuthorizedClientProvider(ReactiveOAuth2AuthorizedClientProvider authorizedClientProvider) {
    Assert.notNull(authorizedClientProvider, "authorizedClientProvider cannot be null");
    this.authorizedClientProvider = authorizedClientProvider;
  }
}
