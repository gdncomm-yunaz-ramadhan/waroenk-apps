import { api } from './client.js';

/**
 * Authentication API service
 */
export const authApi = {
  /**
   * Register a new user
   * @param {Object} data - Registration data
   * @param {string} data.full_name - User's full name
   * @param {string} data.email - User's email
   * @param {string} data.phone - User's phone number
   * @param {string} data.password - User's password
   * @param {string} [data.gender] - User's gender (optional)
   */
  async register(data) {
    return api.post('/user/register', data);
  },

  /**
   * Login user
   * @param {Object} credentials - Login credentials
   * @param {string} credentials.user - Email or phone
   * @param {string} credentials.password - Password
   */
  async login(credentials) {
    return api.post('/user/login', credentials);
  },

  /**
   * Refresh access token using refresh token
   * @param {string} refreshToken - The refresh token
   * @returns {Promise<{access_token: string, refresh_token: string, expires_at: string}>}
   */
  async refreshToken(refreshToken) {
    return api.post('/user/refresh-token', { refresh_token: refreshToken }, { skipAuthRefresh: true });
  },

  /**
   * Get current user profile
   */
  async getProfile() {
    return api.get('/user', { requiresAuth: true });
  },

  /**
   * Update user profile
   * @param {Object} data - Profile update data
   */
  async updateProfile(data) {
    return api.put('/user', data, { requiresAuth: true });
  },

  /**
   * Request password reset (forgot password)
   * @param {string} phoneOrEmail - User's phone number or email
   * @returns {Promise<{success: boolean, message: string, reset_token?: string, expires_in_seconds?: number}>}
   */
  async forgotPassword(phoneOrEmail) {
    return api.post('/user/forgot-password', { phone_or_email: phoneOrEmail });
  },

  /**
   * Change password using reset token
   * @param {Object} data - Change password data
   * @param {string} data.resetToken - The reset token from forgot password
   * @param {string} data.newPassword - New password
   * @param {string} data.confirmPassword - Confirm new password
   * @returns {Promise<{success: boolean, message: string}>}
   */
  async changePassword(data) {
    return api.post('/user/change-password', {
      reset_token: data.resetToken,
      new_password: data.newPassword,
      confirm_password: data.confirmPassword
    });
  }
};

