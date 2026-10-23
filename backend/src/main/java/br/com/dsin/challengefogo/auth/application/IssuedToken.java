package br.com.dsin.challengefogo.auth.application;

/** Token emitido e sua validade em segundos, como o front precisa para agendar o logout. */
public record IssuedToken(String accessToken, long expiresIn) {
}
