package br.com.dsin.challengefogo.shared.web;

import java.net.URI;

/**
 * Tipos de problema (campo {@code type} do RFC 9457). URN em vez de URL porque não há
 * página de documentação publicada; o front distingue os erros por este valor.
 */
public final class ProblemTypes {

    public static final URI VALIDATION = problem("validation");
    public static final URI NOT_FOUND = problem("not-found");
    public static final URI CONFLICT = problem("conflict");
    public static final URI BUSINESS_RULE = problem("business-rule");
    public static final URI INTERNAL = problem("internal");

    private ProblemTypes() {
    }

    private static URI problem(String name) {
        return URI.create("urn:fogo:problem:" + name);
    }
}
