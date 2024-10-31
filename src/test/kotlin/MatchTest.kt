import com.github.schaka.naviseerr.download_client.slskd.dto.SearchFile
import com.github.schaka.naviseerr.download_client.slskd.dto.SearchResult
import com.github.schaka.naviseerr.download_client.slskd.lucene.MatchService
import com.github.schaka.naviseerr.music_library.lidarr.dto.LidarrTrack
import org.junit.jupiter.api.Test

class MatchTest {

    @Test
    fun test() {
        val filename = "m11/TL-0day FLAC OCT17-2024/VA-American_Football_(Covers)-16BIT-WEB-FLAC-2024-ENRiCH/03-novo_amor_lowswimmer-honestly-390f9f20.flac"
        val service = MatchService()

        val files = listOf(SearchFile(filename, 1, "flac"))
        val result = SearchResult(true, 100000, 1, files)
        val track = LidarrTrack("Honestly?", 0, "asdasd-123131-aaaa")
        val matchResult = service.findTrackInSlskd(result, track, "", "")
    }
}