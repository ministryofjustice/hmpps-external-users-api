package uk.gov.justice.digital.hmpps.externalusersapi.config

import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import reactor.core.publisher.Mono

class AuthAwareTokenConverter : Converter<Jwt, Mono<AuthAwareAuthenticationToken>> {
  private val jwtGrantedAuthoritiesConverter: Converter<Jwt, Collection<GrantedAuthority>> =
    JwtGrantedAuthoritiesConverter()

  override fun convert(jwt: Jwt): Mono<AuthAwareAuthenticationToken> {
    val claims = jwt.claims
    val userName = findUserName(claims)
    val clientId = findClientId(claims)
    val authorities = extractAuthorities(jwt)
    return Mono.just(AuthAwareAuthenticationToken(jwt, userName, clientId, authorities))
  }

  private fun findUserName(claims: Map<String, Any?>): String? = if (claims.containsKey("user_name")) {
    claims["user_name"] as String
  } else {
    null
  }

  private fun findClientId(claims: Map<String, Any?>) = claims["client_id"] as String

  private fun extractAuthorities(jwt: Jwt): Collection<GrantedAuthority> {
    val authorities = mutableListOf<GrantedAuthority>().apply { addAll(jwtGrantedAuthoritiesConverter.convert(jwt)!!) }
    if (jwt.claims.containsKey("authorities")) {
      @Suppress("UNCHECKED_CAST")
      val claimAuthorities = (jwt.claims["authorities"] as Collection<String>).toList()
      authorities.addAll(claimAuthorities.map(::SimpleGrantedAuthority))
    }
    return authorities.toSet()
  }
}

class AuthAwareAuthenticationToken(
  jwt: Jwt,
  private val userName: String?,
  private val clientId: String,
  authorities: Collection<GrantedAuthority>,
) : JwtAuthenticationToken(jwt, authorities) {
  override fun getPrincipal(): String = userName ?: clientId
}
