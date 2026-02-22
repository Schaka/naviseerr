import { api } from 'boot/axios'

export interface MediaRequestDto {
  id: string
  artistName: string
  albumTitle?: string
  status: 'REQUESTED' | 'AVAILABLE' | 'FAILED'
  musicbrainzArtistId: string
  musicbrainzAlbumId?: string
  createdAt: string
  updatedAt: string
}

export const requestsApi = {
  async requestArtist(musicbrainzId: string, name: string): Promise<MediaRequestDto> {
    const response = await api.post<MediaRequestDto>('/requests/artist', { musicbrainzId, name })
    return response.data
  },

  async requestAlbum(
    musicbrainzArtistId: string,
    musicbrainzAlbumId: string,
    artistName: string,
    albumTitle: string
  ): Promise<MediaRequestDto> {
    const response = await api.post<MediaRequestDto>('/requests/album', {
      musicbrainzArtistId,
      musicbrainzAlbumId,
      artistName,
      albumTitle,
    })
    return response.data
  },

  async getMyRequests(): Promise<MediaRequestDto[]> {
    const response = await api.get<MediaRequestDto[]>('/requests')
    return response.data
  },

  async getAllRequests(): Promise<MediaRequestDto[]> {
    const response = await api.get<MediaRequestDto[]>('/requests/all')
    return response.data
  },
}
