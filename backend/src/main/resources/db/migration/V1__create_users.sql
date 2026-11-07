-- Application users. keycloak_sub is the validated JWT subject.
CREATE TABLE users (
    id UUID PRIMARY KEY,
    keycloak_sub VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(320) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX users_email_lower_idx ON users (lower(email));
