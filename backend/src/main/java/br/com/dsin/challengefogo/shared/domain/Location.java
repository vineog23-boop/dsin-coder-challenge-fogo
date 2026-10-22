package br.com.dsin.challengefogo.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;

/**
 * Localização embutida, reaproveitada por equipamento e agente (base). Fica embutida
 * em vez de tabela própria porque não tem identidade nem é compartilhada entre registros.
 */
@Embeddable
public class Location {

    @Column(name = "zip_code", length = 8)
    private String zipCode;

    private String street;

    private String reference;

    private String neighborhood;

    private String city;

    @Column(length = 2)
    private String state;

    @Enumerated(EnumType.STRING)
    private Region region;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    protected Location() {
    }

    public Location(String zipCode, String street, String reference, String neighborhood, String city,
                    String state, Region region, BigDecimal latitude, BigDecimal longitude) {
        this.zipCode = zipCode;
        this.street = street;
        this.reference = reference;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getStreet() {
        return street;
    }

    public String getReference() {
        return reference;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public Region getRegion() {
        return region;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }
}
