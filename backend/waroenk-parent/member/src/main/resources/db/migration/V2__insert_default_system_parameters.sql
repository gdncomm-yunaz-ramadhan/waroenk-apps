----------------------------------------------------------------
-- V2: Insert Default System Parameters
-- These parameters are used across the member service
----------------------------------------------------------------

-- Use INSERT ... ON CONFLICT DO NOTHING to make this idempotent
-- (safe to re-run without duplicating data)

----------------------------------------------------------------
-- PASSWORD VALIDATION PARAMETERS
----------------------------------------------------------------

-- Minimum password length requirement
INSERT INTO system_parameters (variable, data, description)
VALUES (
    'MIN_PASSWORD_LENGTH',
    '8',
    'Minimum number of characters required for user passwords'
)
ON CONFLICT (variable) DO NOTHING;

-- Password pattern for strong password validation
-- Pattern requires: lowercase, uppercase, digit, and special character
INSERT INTO system_parameters (variable, data, description)
VALUES (
    'PASSWORD_PATTERN',
    '^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]+$',
    'Regex pattern for password validation. Requires: 1 lowercase, 1 uppercase, 1 digit, 1 special char (@$!%*?&)'
)
ON CONFLICT (variable) DO NOTHING;

----------------------------------------------------------------
-- SESSION & TOKEN PARAMETERS
----------------------------------------------------------------

-- Access token expiration in minutes
INSERT INTO system_parameters (variable, data, description)
VALUES (
    'ACCESS_TOKEN_EXPIRY_MINUTES',
    '30',
    'Access token expiration time in minutes'
)
ON CONFLICT (variable) DO NOTHING;

-- Refresh token expiration in hours
INSERT INTO system_parameters (variable, data, description)
VALUES (
    'REFRESH_TOKEN_EXPIRY_HOURS',
    '24',
    'Refresh token expiration time in hours'
)
ON CONFLICT (variable) DO NOTHING;

