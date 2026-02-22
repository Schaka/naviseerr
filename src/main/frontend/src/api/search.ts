import { api } from 'boot/axios'

export interface ArtistSearchResult {
  musicbrainzId: string
  name: string
  disambiguation?: string
  type?: string
  country?: string
  status?: string
}

export interface AlbumSearchResult {
  musicbrainzId: string
  title: string
  primaryType?: string
  firstReleaseDate?: string
  artistName: string
  artistMusicbrainzId: string
  status?: string
}

export interface SearchResult<T> {
  results: T[]
  totalCount: number
}

export const searchApi = {
  async searchArtists(query: string): Promise<SearchResult<ArtistSearchResult>> {
    const response = await api.get<SearchResult<ArtistSearchResult>>('/search/artists', { params: { query } })
    return response.data
  },

  async searchAlbums(query: string): Promise<SearchResult<AlbumSearchResult>> {
    const response = await api.get<SearchResult<AlbumSearchResult>>('/search/albums', { params: { query } })
    return response.data
  },
}
