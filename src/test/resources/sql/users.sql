CREATE TABLE users
(
    email          text NOT NULL,
    hashedPassword text NOT NULL,
    firstName      text,
    lastName       text,
    company        text,
    role           text NOT NULL
);

ALTER TABLE users
    ADD CONSTRAINT pk_users PRIMARY KEY (email);

INSERT INTO users(email,
                  hashedPassword,
                  firstName,
                  lastName,
                  company,
                  role)
VALUES ('daniel@rockthejvm.com',
        '$2a$10$wg0ZmK8p63u61MO18ZrGeerLwBSBYR2kLSzOM7yUjf/ROYzuuv43m',
        'Daniel',
        'CioCirlan',
        'Rock the JVM',
        'ADMIN');

INSERT INTO users(email,
                  hashedPassword,
                  firstName,
                  lastName,
                  company,
                  role)
VALUES ('riccardo@rockthejvm.com',
        '$2a$10$fQkUtvu5nuOHbrkNltyDs.6TBXthWiRpssPu2bbmkWcFq5MgftZ5.',
        'Riccardo',
        'Cardin',
        'Rock the JVM',
        'RECRUITER');