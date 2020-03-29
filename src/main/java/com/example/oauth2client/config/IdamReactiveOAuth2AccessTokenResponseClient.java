package com.example.oauth2client.config;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Set;
import java.util.function.Consumer;

import reactor.core.publisher.Mono;

import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.GRANT_TYPE;
import static org.springframework.security.oauth2.core.web.reactive.function.OAuth2BodyExtractors.oauth2AccessTokenResponse;

public class IdamReactiveOAuth2AccessTokenResponseClient implements
        ReactiveOAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> {

  private static final String REALM_ID = "realm_id";

  private WebClient webClient = WebClient.builder()
          .build();

  @Override
  public Mono<OAuth2AccessTokenResponse> getTokenResponse
          (OAuth2ClientCredentialsGrantRequest authorizationGrantRequest) {
    return Mono.defer(() -> {
      ClientRegistration clientRegistration = authorizationGrantRequest.getClientRegistration();

      String tokenUri = clientRegistration.getProviderDetails().getTokenUri();
      BodyInserters.FormInserter<String> body = body(authorizationGrantRequest);

      return this.webClient.post()
              .uri(tokenUri)
              .accept(MediaType.APPLICATION_JSON)
              .attribute(OAuth2AuthorizationCodeGrantRequest.class.getName(), authorizationGrantRequest)
              .headers(headers(clientRegistration))
              .body(body)
              .exchange()
              .flatMap(response -> {
                HttpStatus status = HttpStatus.resolve(response.rawStatusCode());
                if (status == null || !status.is2xxSuccessful()) {
                  // extract the contents of this into a method named oauth2AccessTokenResponse but has an argument for the response
                  return response.bodyToFlux(DataBuffer.class)
                          .map(DataBufferUtils::release)
                          .then(Mono.error(WebClientResponseException.create(response.rawStatusCode(),
                                  "Cannot get token, expected 2xx HTTP Status code",
                                  null,
                                  null,
                                  null
                          )));
                }
                return response.body(oauth2AccessTokenResponse());
              })
              .map(response -> {
                if (response.getAccessToken().getScopes().isEmpty()) {
                  response = OAuth2AccessTokenResponse.withResponse(response)
                          .scopes(authorizationGrantRequest.getClientRegistration().getScopes())
                          .build();
                }
                return response;
              });
    });
  }

  private Consumer<HttpHeaders> headers(ClientRegistration clientRegistration) {
    return headers -> {
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      if (ClientAuthenticationMethod.BASIC.equals(clientRegistration.getClientAuthenticationMethod())) {
        headers.setBasicAuth(clientRegistration.getClientId(), clientRegistration.getClientSecret());
      }
    };
  }

  private static BodyInserters.FormInserter<String> body(OAuth2ClientCredentialsGrantRequest authorizationGrantRequest) {
    ClientRegistration clientRegistration = authorizationGrantRequest.getClientRegistration();
    MultiValueMap<String, String> formParameters = new LinkedMultiValueMap<>();
    formParameters.add(GRANT_TYPE, authorizationGrantRequest.getGrantType().getValue());
    formParameters.add(REALM_ID, "ALEX_REALM");
    /*BodyInserters.FormInserter<String> body = BodyInserters
            .fromFormData(OAuth2ParameterNames.GRANT_TYPE, authorizationGrantRequest.getGrantType().getValue());*/
    BodyInserters.FormInserter<String> body = BodyInserters.fromFormData(formParameters);
    Set<String> scopes = clientRegistration.getScopes();
    if (!CollectionUtils.isEmpty(scopes)) {
      String scope = StringUtils.collectionToDelimitedString(scopes, " ");
      body.with(OAuth2ParameterNames.SCOPE, scope);
    }
    if (ClientAuthenticationMethod.POST.equals(clientRegistration.getClientAuthenticationMethod())) {
      body.with(OAuth2ParameterNames.CLIENT_ID, clientRegistration.getClientId());
      body.with(OAuth2ParameterNames.CLIENT_SECRET, clientRegistration.getClientSecret());
    }
    return body;
  }

  public void setWebClient(WebClient webClient) {
    Assert.notNull(webClient, "webClient cannot be null");
    this.webClient = webClient;
  }
}
