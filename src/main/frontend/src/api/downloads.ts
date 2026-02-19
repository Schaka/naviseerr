import { api } from 'boot/axios'

export interface DownloadItem {
  id: number
  artistName: string
  albumTitle: string
  progress: number
  status: string
  timeleft?: string
  estimatedCompletionTime?: string
  protocol?: string
}

export interface DownloadQueue {
  items: DownloadItem[]
  totalRecords: number
}

export const downloadsApi = {
  async getQueue(page = 0, pageSize = 20): Promise<DownloadQueue> {
    const response = await api.get<DownloadQueue>('/downloads/queue', { params: { page, pageSize } })
    return response.data
  },
}
