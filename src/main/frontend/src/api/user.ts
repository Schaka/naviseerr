import { api } from 'boot/axios'

export interface UserSettings {
  username: string
  lastFmSessionKey?: string
  listenBrainzToken?: string
  lastFmScrobblingEnabled: boolean
  listenBrainzScrobblingEnabled: boolean
}

export interface UpdateUserSettingsRequest {
  listenBrainzToken?: string
  lastFmScrobblingEnabled?: boolean
  listenBrainzScrobblingEnabled?: boolean
}

export interface LastFmAuthResult {
  success: boolean
  message: string
  sessionKey?: string
}

export interface BackfillResult {
  success: boolean
  message: string
  newPlaysCount: number
  requestedCount: number
}

export const userApi = {
  async getUserSettings(): Promise<UserSettings> {
    const response = await api.get<UserSettings>('/user/settings')
    return response.data
  },

  async updateUserSettings(settings: UpdateUserSettingsRequest): Promise<UserSettings> {
    const response = await api.put<UserSettings>('/user/settings', settings)
    return response.data
  },

  /**
   * Initiates Last.fm OAuth flow by returning the authorization URL.
   * User should be redirected to this URL to authorize the application.
   */
  getLastFmAuthUrl(callbackUrl: string): string {
    // Construct the auth init URL with callback
    const params = new URLSearchParams({ callback_url: callbackUrl })
    return `${api.defaults.baseURL}/user/lastfm/auth/init?${params.toString()}`
  },

  /**
   * Handles Last.fm OAuth callback by exchanging token for session key.
   */
  async handleLastFmCallback(token: string): Promise<LastFmAuthResult> {
    const response = await api.get<LastFmAuthResult>('/user/lastfm/auth/callback', {
      params: { token }
    })
    return response.data
  },

  /**
   * Backfills listening history from Navidrome.
   * Fetches recent plays and stores them locally.
   */
  async backfillActivity(count: number = 100): Promise<BackfillResult> {
    const response = await api.post<BackfillResult>('/user/backfill', null, {
      params: { count }
    })
    return response.data
  },
}
