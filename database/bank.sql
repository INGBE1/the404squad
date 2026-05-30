-- Base de donnees generee par the404squad - gestionnaire de compte
-- Titulaire : Lina Moreau, 20 ans, Bruxelles (Ixelles)
-- IBAN : BE71 0961 2345 6769

DROP TABLE IF EXISTS transaction;
DROP TABLE IF EXISTS account;

CREATE TABLE account (
    id               INTEGER PRIMARY KEY,
    holder           VARCHAR(100) NOT NULL,
    age              INTEGER NOT NULL,
    city             VARCHAR(100) NOT NULL,
    iban             VARCHAR(40)  NOT NULL,
    balance          DECIMAL(10,2) NOT NULL
);

CREATE TABLE transaction (
    id        INTEGER PRIMARY KEY,
    op_date   DATE NOT NULL,
    label     VARCHAR(120) NOT NULL,
    amount    DECIMAL(10,2) NOT NULL,
    category  VARCHAR(40) NOT NULL,
    kind      VARCHAR(20) NOT NULL
);

INSERT INTO account VALUES (1, 'Lina Moreau', 20, 'Bruxelles (Ixelles)', 'BE71 0961 2345 6769', 2000,00);

INSERT INTO transaction (id, op_date, label, amount, category, kind) VALUES
  (1, '2026-05-01', 'Salaire', 2000,00, 'REVENU', 'REVENU');
