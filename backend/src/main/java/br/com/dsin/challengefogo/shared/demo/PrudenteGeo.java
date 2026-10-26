package br.com.dsin.challengefogo.shared.demo;

import br.com.dsin.challengefogo.shared.domain.Location;
import br.com.dsin.challengefogo.shared.domain.Region;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Geografia aproximada de Presidente Prudente/SP para os dados de demonstração. Os pontos caem dentro
 * da área urbana, e a região sai do quadrante em relação ao centro, a mesma regra do enum {@link Region}.
 * Bairros e ruas são reais, mas a combinação rua/bairro/CEP é ilustrativa.
 */
final class PrudenteGeo {

    static final double CENTER_LAT = -22.125600;
    static final double CENTER_LNG = -51.388900;
    static final double HALF_HEIGHT = 0.035;
    static final double HALF_WIDTH = 0.045;

    /** Raio normalizado do "centro": abaixo dele, a região é {@code CENTER}. */
    private static final double CENTER_RADIUS = 0.3;

    private static final Map<Region, List<String>> NEIGHBORHOODS = Map.of(
            Region.CENTER, List.of("Centro", "Vila Marcondes", "Vila Roberto", "Jardim Paulista"),
            Region.NORTH, List.of("Jardim Humberto Salvador", "Brasil Novo", "Jardim Morada do Sol", "Vila Industrial"),
            Region.SOUTH, List.of("Jardim Bongiovani", "Parque Higienópolis", "Jardim São Paulo", "Jardim Itapura"),
            Region.EAST, List.of("Vila Furquim", "Parque Furquim", "Jardim Planalto", "Cecap"),
            Region.WEST, List.of("Ana Jacinta", "Jardim Cambuy", "Parque São Judas Tadeu", "Jardim Prudentino"));

    private static final List<String> STREETS = List.of(
            "Avenida Washington Luiz", "Avenida Brasil", "Avenida Manoel Goulart",
            "Avenida Coronel José Soares Marcondes", "Avenida Ana Jacinta", "Avenida Juscelino Kubitschek",
            "Rua Tenente Nicolau Maffei", "Avenida Onze de Maio", "Rua Barão do Rio Branco",
            "Avenida Joaquim Constantino", "Rua Siqueira Campos", "Avenida Celestino Figueiredo");

    private static final List<String> LANDMARKS = List.of(
            "em frente ao Mercado do Tio", "ao lado da praça", "próximo ao terminal", "esquina com a avenida");

    private PrudenteGeo() {
    }

    static Location randomLocation(Random random) {
        double lat = CENTER_LAT + (random.nextDouble() * 2 - 1) * HALF_HEIGHT;
        double lng = CENTER_LNG + (random.nextDouble() * 2 - 1) * HALF_WIDTH;
        Region region = regionOf(lat, lng);
        List<String> neighborhoods = NEIGHBORHOODS.get(region);

        return new Location(
                "190" + String.format("%05d", random.nextInt(100_000)),
                pick(random, STREETS),
                reference(random),
                pick(random, neighborhoods),
                "Presidente Prudente",
                "SP",
                region,
                BigDecimal.valueOf(lat).setScale(6, RoundingMode.HALF_UP),
                BigDecimal.valueOf(lng).setScale(6, RoundingMode.HALF_UP));
    }

    static Region regionOf(double lat, double lng) {
        double dy = (lat - CENTER_LAT) / HALF_HEIGHT;
        double dx = (lng - CENTER_LNG) / HALF_WIDTH;
        if (Math.hypot(dx, dy) < CENTER_RADIUS) {
            return Region.CENTER;
        }
        if (Math.abs(dy) > Math.abs(dx)) {
            return dy > 0 ? Region.NORTH : Region.SOUTH;
        }
        return dx > 0 ? Region.EAST : Region.WEST;
    }

    private static String reference(Random random) {
        int number = 10 + random.nextInt(2_000);
        return switch (random.nextInt(5)) {
            case 0 -> "oposto ao " + number;
            case 1 -> "S/N";
            case 2 -> pick(random, LANDMARKS);
            default -> "número " + number;
        };
    }

    static <T> T pick(Random random, List<T> values) {
        return values.get(random.nextInt(values.size()));
    }
}
