-- V1__baseline.sql
-- Baseline schema for Investment App (MySQL)

-- 1) Address table (mapped by Address @Table(name="address"))
CREATE TABLE IF NOT EXISTS address (
  id INT NOT NULL AUTO_INCREMENT,
  street VARCHAR(45) NOT NULL,
  state VARCHAR(2) NOT NULL,
  zipcode INT NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

-- 2) Users table (mapped by User @Table(name="myusers"))
-- User has a OneToOne to Address via @JoinColumn(name="address", referencedColumnName="id")
CREATE TABLE IF NOT EXISTS myusers (
  id INT NOT NULL AUTO_INCREMENT,
  first_name VARCHAR(30) NOT NULL,
  last_name VARCHAR(30) NOT NULL,
  email VARCHAR(255) NOT NULL,
  password VARCHAR(100) NOT NULL,
  address INT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_myusers_email (email),
  CONSTRAINT fk_myusers_address
    FOREIGN KEY (address) REFERENCES address(id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 3) Stocks table (mapped by Stock @Table(name="stocks"))
CREATE TABLE IF NOT EXISTS stocks (
  id INT NOT NULL AUTO_INCREMENT,
  stock_name VARCHAR(45) NOT NULL,
  ticker VARCHAR(10) NOT NULL,
  price DECIMAL(12,2) NOT NULL,
  description VARCHAR(100) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_stocks_ticker (ticker)
) ENGINE=InnoDB;

-- 4) Possessions table (mapped by Possession @Table(name="possessions"))
-- Possession has:
--  @JoinColumn(name="myuser_id") -> myusers(id)
--  @JoinColumn(name="investment_id") -> stocks(id)
CREATE TABLE IF NOT EXISTS possessions (
  id INT NOT NULL AUTO_INCREMENT,
  shares DOUBLE NOT NULL,
  myuser_id INT NOT NULL,
  investment_id INT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_possessions_user_stock (myuser_id, investment_id),
  KEY ix_possessions_myuser_id (myuser_id),
  KEY ix_possessions_investment_id (investment_id),
  CONSTRAINT fk_possessions_myuser
    FOREIGN KEY (myuser_id) REFERENCES myusers(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT fk_possessions_stock
    FOREIGN KEY (investment_id) REFERENCES stocks(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 5) Join table used by User.userPossessions:
-- @JoinTable(name="myuser_portfolio", joinColumns myuser_id, inverse possession_id)
CREATE TABLE IF NOT EXISTS myuser_portfolio (
  myuser_id INT NOT NULL,
  possession_id INT NOT NULL,
  PRIMARY KEY (myuser_id, possession_id),
  KEY ix_myuser_portfolio_possession (possession_id),
  CONSTRAINT fk_myuser_portfolio_user
    FOREIGN KEY (myuser_id) REFERENCES myusers(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT fk_myuser_portfolio_possession
    FOREIGN KEY (possession_id) REFERENCES possessions(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 6) Join table used by Stock.userStocks:
-- @JoinTable(name="stocks_and_possessions", joinColumns stock_id, inverse possession_id)
CREATE TABLE IF NOT EXISTS stocks_and_possessions (
  stock_id INT NOT NULL,
  possession_id INT NOT NULL,
  PRIMARY KEY (stock_id, possession_id),
  KEY ix_stocks_and_possessions_possession (possession_id),
  CONSTRAINT fk_stocks_and_possessions_stock
    FOREIGN KEY (stock_id) REFERENCES stocks(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT fk_stocks_and_possessions_possession
    FOREIGN KEY (possession_id) REFERENCES possessions(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 7) AuthGroup entity (no explicit @Table, so default table name is typically auth_group)
CREATE TABLE IF NOT EXISTS auth_group (
  id INT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  role VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  KEY ix_auth_group_email (email)
) ENGINE=InnoDB;
