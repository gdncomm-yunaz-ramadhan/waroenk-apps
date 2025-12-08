----------------------------------------------------------------
-- V3: Add Password Requirements Message Parameter
----------------------------------------------------------------

-- Human-readable password requirements message
-- This is shown to users when their password doesn't meet requirements
INSERT INTO system_parameters (variable, data, description)
VALUES (
    'PASSWORD_REQUIREMENTS',
    'Password must contain uppercase, lowercase, number, and special character (@$!%*?&)',
    'User-friendly message shown when password does not meet requirements'
)
ON CONFLICT (variable) DO NOTHING;














