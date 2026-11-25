-- Dompetku JSP - physical data model
-- Engine: InnoDB, charset utf8mb4

CREATE TABLE IF NOT EXISTS users (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100)  NOT NULL,
  email         VARCHAR(150)  NOT NULL UNIQUE,
  password_hash VARCHAR(255)  NOT NULL,
  session_version INT         NOT NULL DEFAULT 0,
  email_verified_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS categories (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  user_id    INT          NOT NULL,
  name       VARCHAR(60)  NOT NULL,
  type       ENUM('income','expense') NOT NULL,
  color      CHAR(7)      NOT NULL DEFAULT '#10b981',
  icon       VARCHAR(40)  NOT NULL DEFAULT 'tag',
  created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_category_user_name_type (user_id, name, type),
  KEY idx_category_user (user_id),
  CONSTRAINT fk_category_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS wallets (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  user_id         INT           NOT NULL,
  name            VARCHAR(80)   NOT NULL,
  type            ENUM('cash','bank','ewallet') NOT NULL,
  initial_balance DECIMAL(15,2) NOT NULL DEFAULT 0,
  color           CHAR(7)       NOT NULL DEFAULT '#2563eb',
  created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_wallet_initial CHECK (initial_balance >= 0),
  KEY idx_wallet_user (user_id),
  CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS transactions (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  user_id          INT           NOT NULL,
  wallet_id        INT           NOT NULL,
  category_id      INT           NULL,
  type             ENUM('income','expense') NOT NULL,
  amount           DECIMAL(15,2) NOT NULL,
  transaction_date DATE          NOT NULL,
  note             VARCHAR(255)  NULL,
  created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_transaction_amount CHECK (amount > 0),
  KEY idx_transaction_user_date (user_id, transaction_date),
  KEY idx_transaction_wallet (wallet_id),
  KEY idx_transaction_category (category_id),
  CONSTRAINT fk_transaction_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_transaction_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE,
  CONSTRAINT fk_transaction_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS transactions_archive (
  id               INT           NOT NULL PRIMARY KEY,
  user_id          INT           NOT NULL,
  wallet_id        INT           NOT NULL,
  category_id      INT           NULL,
  type             ENUM('income','expense') NOT NULL,
  amount           DECIMAL(15,2) NOT NULL,
  transaction_date DATE          NOT NULL,
  note             VARCHAR(255)  NULL,
  created_at       TIMESTAMP     NOT NULL,
  deleted_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_by       INT           NULL,
  KEY idx_archive_user_date (user_id, transaction_date),
  KEY idx_archive_wallet (wallet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_logs (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  user_id     INT          NOT NULL,
  action      VARCHAR(20)  NOT NULL,
  entity_type VARCHAR(30)  NOT NULL,
  entity_id   INT          NULL,
  details     VARCHAR(500) NOT NULL,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_audit_user_created (user_id, created_at),
  CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS auth_tokens (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  user_id    INT          NOT NULL,
  purpose    ENUM('password_reset','email_verification') NOT NULL,
  token_hash CHAR(64)     NOT NULL,
  expires_at TIMESTAMP    NOT NULL,
  used_at    TIMESTAMP    NULL,
  created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_auth_token_hash (token_hash),
  KEY idx_auth_user_purpose (user_id, purpose),
  CONSTRAINT fk_auth_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS budgets (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  user_id     INT           NOT NULL,
  category_id INT           NOT NULL,
  month       DATE          NOT NULL,
  amount      DECIMAL(15,2) NOT NULL,
  created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_budget_amount CHECK (amount > 0),
  CONSTRAINT chk_budget_month CHECK (DAY(month) = 1),
  UNIQUE KEY uq_budget_user_category_month (user_id, category_id, month),
  KEY idx_budget_user_month (user_id, month),
  CONSTRAINT fk_budget_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_budget_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS goals (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  user_id       INT           NOT NULL,
  name          VARCHAR(100)  NOT NULL,
  target_amount DECIMAL(15,2) NOT NULL,
  saved_amount  DECIMAL(15,2) NOT NULL DEFAULT 0,
  deadline      DATE          NULL,
  color         CHAR(7)       NOT NULL DEFAULT '#7c3aed',
  created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_goal_target CHECK (target_amount > 0),
  CONSTRAINT chk_goal_saved CHECK (saved_amount >= 0),
  KEY idx_goal_user (user_id),
  CONSTRAINT fk_goal_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS goal_deposits (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  goal_id      INT           NOT NULL,
  amount       DECIMAL(15,2) NOT NULL,
  deposit_date DATE          NOT NULL,
  note         VARCHAR(255)  NULL,
  created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_deposit_amount CHECK (amount > 0),
  KEY idx_deposit_goal (goal_id),
  CONSTRAINT fk_deposit_goal FOREIGN KEY (goal_id) REFERENCES goals(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS debts (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  user_id     INT           NOT NULL,
  type        ENUM('payable','receivable') NOT NULL,
  counterpart VARCHAR(100)  NOT NULL,
  amount      DECIMAL(15,2) NOT NULL,
  paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
  due_date    DATE          NULL,
  note        VARCHAR(255)  NULL,
  created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_debt_amount CHECK (amount > 0),
  CONSTRAINT chk_debt_paid CHECK (paid_amount >= 0 AND paid_amount <= amount),
  KEY idx_debt_user (user_id),
  CONSTRAINT fk_debt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recurring_transactions (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  user_id     INT           NOT NULL,
  wallet_id   INT           NOT NULL,
  category_id INT           NULL,
  name        VARCHAR(100)  NOT NULL,
  type        ENUM('income','expense') NOT NULL,
  amount      DECIMAL(15,2) NOT NULL,
  frequency   ENUM('daily','weekly','monthly') NOT NULL,
  next_date   DATE          NOT NULL,
  active      TINYINT(1)    NOT NULL DEFAULT 1,
  created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_recurring_amount CHECK (amount > 0),
  KEY idx_recurring_user (user_id),
  CONSTRAINT fk_recurring_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_recurring_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE,
  CONSTRAINT fk_recurring_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
