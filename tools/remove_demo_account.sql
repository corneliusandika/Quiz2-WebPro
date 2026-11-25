-- Run only against a production database after confirming the demo account is not needed.
-- This removes the public demo login and its personal data.
DELETE FROM transactions_archive
 WHERE user_id IN (SELECT id FROM users WHERE email = 'demo@dompetku.test');
DELETE FROM users WHERE email = 'demo@dompetku.test';
