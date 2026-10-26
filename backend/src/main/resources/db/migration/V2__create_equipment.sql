-- Herança JOINED: dados comuns em equipment; cada tabela filha usa o mesmo id (PK e FK).
-- Tipos sem características próprias (AUTOMATED_GATE, PARKING_KIOSK, OTHER) ficam só em equipment.
CREATE TABLE equipment (
    id                        UUID PRIMARY KEY,
    type                      VARCHAR(40)  NOT NULL CHECK (type IN (
                                  'RADAR', 'SURVEILLANCE_CAMERA', 'TRUCK_SCALE', 'TAXI', 'MOTO_TAXI',
                                  'SCHOOL_TRANSPORT', 'PARKING_ENFORCEMENT_VEHICLE', 'TRAFFIC_AGENT_MOTORCYCLE',
                                  'MUNICIPAL_GUARD_VEHICLE', 'MILITARY_POLICE_VEHICLE', 'ELECTRIC_SCOOTER',
                                  'AUTOMATED_GATE', 'PARKING_KIOSK', 'OTHER')),
    serial_number             VARCHAR(60)  NOT NULL UNIQUE,
    brand                     VARCHAR(60)  NOT NULL,
    model                     VARCHAR(60)  NOT NULL,
    agency                    VARCHAR(30)  NOT NULL CHECK (agency IN (
                                  'MUNICIPAL_TRAFFIC', 'MUNICIPAL_GUARD', 'MILITARY_POLICE', 'PRIVATE_OPERATOR')),
    status                    VARCHAR(30)  NOT NULL CHECK (status IN (
                                  'NOT_INSTALLED', 'OPERATIONAL', 'MALFUNCTION', 'UNDER_MAINTENANCE',
                                  'DEACTIVATED', 'STOLEN')),
    maintenance_need          VARCHAR(20)  NOT NULL CHECK (maintenance_need IN ('NONE', 'IMMEDIATE', 'SCHEDULED')),
    next_maintenance_date     DATE,
    maintenance_interval_days INTEGER CHECK (maintenance_interval_days > 0),
    zip_code                  VARCHAR(8),
    street                    VARCHAR(120),
    reference                 VARCHAR(120),
    neighborhood              VARCHAR(80),
    city                      VARCHAR(80),
    state                     VARCHAR(2),
    region                    VARCHAR(10) CHECK (region IN ('CENTER', 'NORTH', 'SOUTH', 'EAST', 'WEST')),
    latitude                  NUMERIC(9, 6),
    longitude                 NUMERIC(9, 6),
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                TIMESTAMP WITH TIME ZONE NOT NULL,
    -- Regra 6 da spec, garantida também no banco.
    CONSTRAINT ck_equipment_scheduled_date CHECK (maintenance_need <> 'SCHEDULED' OR next_maintenance_date IS NOT NULL)
);

CREATE INDEX ix_equipment_status ON equipment (status);
CREATE INDEX ix_equipment_type ON equipment (type);
CREATE INDEX ix_equipment_region ON equipment (region);

CREATE TABLE radar (
    id              UUID PRIMARY KEY REFERENCES equipment (id),
    modality        VARCHAR(20) NOT NULL CHECK (modality IN (
                        'SPEED', 'RED_LIGHT', 'ILLEGAL_TURN', 'ILLEGAL_U_TURN', 'BUS_LANE')),
    speed_limit_kmh INTEGER CHECK (speed_limit_kmh > 0),
    lanes           INTEGER CHECK (lanes > 0),
    CONSTRAINT ck_radar_speed_limit CHECK (modality <> 'SPEED' OR speed_limit_kmh IS NOT NULL)
);

CREATE TABLE camera (
    id           UUID PRIMARY KEY REFERENCES equipment (id),
    night_vision BOOLEAN     NOT NULL,
    resolution   VARCHAR(20) NOT NULL CHECK (resolution IN ('HD_720', 'FULL_HD_1080', 'QHD_1440', 'UHD_4K')),
    color        BOOLEAN     NOT NULL,
    ptz          BOOLEAN     NOT NULL
);

CREATE TABLE truck_scale (
    id                UUID PRIMARY KEY REFERENCES equipment (id),
    max_capacity_tons NUMERIC(6, 2) NOT NULL CHECK (max_capacity_tons > 0),
    weigh_in_motion   BOOLEAN       NOT NULL
);

CREATE TABLE vehicle (
    id                 UUID PRIMARY KEY REFERENCES equipment (id),
    license_plate      VARCHAR(7) NOT NULL UNIQUE,
    passenger_capacity INTEGER CHECK (passenger_capacity >= 0),
    autonomous         BOOLEAN    NOT NULL
);

CREATE TABLE scooter (
    id            UUID PRIMARY KEY REFERENCES equipment (id),
    ownership     VARCHAR(10) NOT NULL CHECK (ownership IN ('PUBLIC', 'SHARED')),
    operator_name VARCHAR(100),
    battery_level INTEGER CHECK (battery_level BETWEEN 0 AND 100),
    CONSTRAINT ck_scooter_operator CHECK (ownership <> 'SHARED' OR operator_name IS NOT NULL)
);

-- Histórico desde a Parte 1: é o insumo das previsões da Parte 3.
CREATE TABLE equipment_status_history (
    id           UUID PRIMARY KEY,
    equipment_id UUID        NOT NULL REFERENCES equipment (id),
    from_status  VARCHAR(30),
    to_status    VARCHAR(30) NOT NULL,
    changed_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    changed_by   VARCHAR(50) NOT NULL,
    note         VARCHAR(500)
);

CREATE INDEX ix_equipment_status_history_equipment ON equipment_status_history (equipment_id, changed_at DESC);

CREATE TABLE equipment_maintenance (
    id           UUID PRIMARY KEY,
    equipment_id UUID         NOT NULL REFERENCES equipment (id),
    kind         VARCHAR(20)  NOT NULL CHECK (kind IN ('CORRECTIVE', 'PREVENTIVE')),
    performed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    description  VARCHAR(500) NOT NULL,
    performed_by VARCHAR(50)  NOT NULL
);

CREATE INDEX ix_equipment_maintenance_equipment ON equipment_maintenance (equipment_id, performed_at DESC);
