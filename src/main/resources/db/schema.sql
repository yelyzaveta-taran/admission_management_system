CREATE TABLE application_status (
    status_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE
);

CREATE TABLE educational_program (
    program_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    duration_months INTEGER NOT NULL CHECK (duration_months > 0),
    complexity_level VARCHAR(32) NOT NULL CHECK (
        complexity_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')
    )
);

CREATE TABLE application (
    application_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    email VARCHAR(255) NOT NULL,
    comment TEXT,
    program_id INT NOT NULL,
    status_id INT NOT NULL,
    datetime TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_application_program
        FOREIGN KEY (program_id) REFERENCES educational_program (program_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_application_status
        FOREIGN KEY (status_id) REFERENCES application_status (status_id)
        ON DELETE RESTRICT
);

CREATE TABLE application_communication (
    event_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id INT NOT NULL,
    channel VARCHAR(32) NOT NULL CHECK (
        channel IN ('EMAIL', 'PHONE', 'TELEGRAM', 'VIBER')
    ),
    result VARCHAR(32) NOT NULL CHECK (
        result IN ('NO_ANSWER', 'ANSWERED', 'CONTACT_LATER', 'WRONG_CONTACT')
    ),
    comment TEXT,
    datetime TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_application_communication_application
        FOREIGN KEY (application_id) REFERENCES application (application_id)
        ON DELETE CASCADE
);

CREATE TABLE application_status_change (
    event_id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    application_id INT NOT NULL,
    previous_status_id INT NOT NULL,
    new_status_id INT NOT NULL,
    reason TEXT,
    datetime TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_status_change_application
        FOREIGN KEY (application_id) REFERENCES application (application_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_status_change_previous_status
        FOREIGN KEY (previous_status_id) REFERENCES application_status (status_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_status_change_new_status
        FOREIGN KEY (new_status_id) REFERENCES application_status (status_id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_status_change_different_statuses
        CHECK (previous_status_id <> new_status_id)
);
