----------------------------------------------------------------
-- V5: Add Reset Token Expiry System Parameter
-- This parameter controls the TTL of password reset tokens
----------------------------------------------------------------

-- Reset token expiration in minutes (default 15 minutes)
INSERT INTO system_parameters (variable, data, description)
VALUES (
    'RESET_TOKEN_EXPIRY_MINUTES',
    '15',
    'Password reset token expiration time in minutes. Reset tokens stored in Redis will expire after this duration.'
)
ON CONFLICT (variable) DO NOTHING;









