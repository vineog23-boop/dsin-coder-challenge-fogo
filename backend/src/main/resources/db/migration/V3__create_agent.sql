-- Agente com base embutida (mesmas colunas de localização do equipamento) e viatura opcional.
CREATE TABLE agent (
    id                  UUID PRIMARY KEY,
    name                VARCHAR(120) NOT NULL,
    registration_number VARCHAR(30)  NOT NULL UNIQUE,
    agent_type          VARCHAR(20)  NOT NULL CHECK (agent_type IN ('TRAFFIC_AGENT', 'MUNICIPAL_GUARD', 'MILITARY_POLICE')),
    agency              VARCHAR(30)  NOT NULL CHECK (agency IN (
                            'MUNICIPAL_TRAFFIC', 'MUNICIPAL_GUARD', 'MILITARY_POLICE', 'PRIVATE_OPERATOR')),
    status              VARCHAR(20)  NOT NULL CHECK (status IN ('AVAILABLE', 'ON_OCCURRENCE', 'OFF_DUTY', 'ON_LEAVE')),
    zip_code            VARCHAR(8),
    street              VARCHAR(120),
    reference           VARCHAR(120),
    neighborhood        VARCHAR(80),
    city                VARCHAR(80),
    state               VARCHAR(2),
    region              VARCHAR(10) CHECK (region IN ('CENTER', 'NORTH', 'SOUTH', 'EAST', 'WEST')),
    latitude            NUMERIC(9, 6),
    longitude           NUMERIC(9, 6),
    current_latitude    NUMERIC(9, 6),
    current_longitude   NUMERIC(9, 6),
    position_updated_at TIMESTAMP WITH TIME ZONE,
    vehicle_id          UUID REFERENCES vehicle (id),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    -- Regra 5 da spec, garantida também no banco: fora de serviço ou afastado não fica com viatura.
    CONSTRAINT ck_agent_vehicle_on_duty CHECK (status IN ('AVAILABLE', 'ON_OCCURRENCE') OR vehicle_id IS NULL)
);

CREATE INDEX ix_agent_status ON agent (status);
CREATE INDEX ix_agent_agency ON agent (agency);
CREATE INDEX ix_agent_region ON agent (region);
CREATE INDEX ix_agent_vehicle ON agent (vehicle_id);

CREATE TABLE agent_status_history (
    id          UUID PRIMARY KEY,
    agent_id    UUID        NOT NULL REFERENCES agent (id),
    from_status VARCHAR(20),
    to_status   VARCHAR(20) NOT NULL,
    changed_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    changed_by  VARCHAR(50) NOT NULL,
    note        VARCHAR(500)
);

CREATE INDEX ix_agent_status_history_agent ON agent_status_history (agent_id, changed_at DESC);
