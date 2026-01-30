-- V2__seed_data.sql
-- Seeds the stocks table in an idempotent way:
-- re-running migrations will NOT duplicate rows (updates existing tickers instead).

INSERT INTO stocks (stock_name, ticker, price, description) VALUES
  ('Apple Inc.', 'AAPL', 128.28, 'Technology company.'),
  ('Microsoft Corporation', 'MSFT', 240.18, 'Technology company.'),
  ('Amazon.com, Inc.', 'AMZN', 3127.47, 'Technology and retail company.'),
  ('Alphabet Inc.', 'GOOGL', 2075.95, 'Technology company.'),
  ('Facebook, Inc.', 'FB', 264.28, 'Social media and technology company.'),
  ('Tesla, Inc.', 'TSLA', 789.30, 'Electric vehicle and clean energy company.'),
  ('Johnson & Johnson', 'JNJ', 165.89, 'Pharmaceutical and consumer goods company.'),
  ('JPMorgan Chase & Co.', 'JPM', 152.14, 'Investment banking and financial services.'),
  ('Exxon Mobil Corporation', 'XOM', 56.23, 'Oil and gas company.'),
  ('Berkshire Hathaway Inc.', 'BRK.A', 408548.00, 'Conglomerate holding company.'),
  ('Visa Inc.', 'V', 206.72, 'Financial services company.'),
  ('Walmart Inc.', 'WMT', 138.61, 'Retail corporation.'),
  ('Johnson Controls International plc', 'JCI', 65.88, 'Diversified technology and industrial company.'),
  ('McDonald''s Corporation', 'MCD', 209.64, 'Fast food restaurant chain.'),
  ('The Goldman Sachs Group, Inc.', 'GS', 348.15, 'Investment banking and financial services.'),
  ('Intel Corporation', 'INTC', 63.62, 'Technology company.'),
  ('Cisco Systems, Inc.', 'CSCO', 45.95, 'Networking and IT company.'),
  ('Walt Disney Co.', 'DIS', 185.27, 'Entertainment and media company.'),
  ('Pfizer Inc.', 'PFE', 35.77, 'Pharmaceuticals company.'),
  ('Coca-Cola Consolidated, Inc.', 'COKE', 345.38, 'Soft drink bottling company.'),
  ('Verizon Communications Inc.', 'VZ', 56.60, 'Telecommunications company.'),
  ('UnitedHealth Group Incorporated', 'UNH', 350.23, 'Healthcare services and insurance company.'),
  ('Procter & Gamble Co.', 'PG', 129.84, 'Consumer goods company.'),
  ('The Home Depot, Inc.', 'HD', 293.18, 'Home improvement retailer.'),
  ('General Electric Company', 'GE', 12.52, 'Multinational conglomerate.')
ON DUPLICATE KEY UPDATE
  stock_name = VALUES(stock_name),
  price = VALUES(price),
  description = VALUES(description);
