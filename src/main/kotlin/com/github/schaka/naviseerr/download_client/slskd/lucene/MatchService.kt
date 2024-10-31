package com.github.schaka.naviseerr.download_client.slskd.lucene

import com.github.schaka.naviseerr.download_client.slskd.dto.SearchFile
import com.github.schaka.naviseerr.download_client.slskd.dto.SearchResult
import com.github.schaka.naviseerr.music_library.lidarr.dto.LidarrTrack
import org.apache.commons.text.similarity.LevenshteinDistance
import org.apache.lucene.analysis.core.LowerCaseFilterFactory
import org.apache.lucene.analysis.custom.CustomAnalyzer
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.apache.lucene.analysis.miscellaneous.TrimFilterFactory
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory
import org.apache.lucene.analysis.standard.StandardTokenizerFactory
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.TextField.TYPE_STORED
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.store.ByteBuffersDirectory
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.util.*
import kotlin.math.abs

@Service
class MatchService {

    private val yearRegex = Regex("\\s?[\\[(]\\d{4}[)\\]]")
    // this needs to be a fallback value we can always find again, but not a string that can be randomly contained in a song name
    private val impossibleSearchResult: UUID = UUID.randomUUID()

    private val indexAnalyzer = CustomAnalyzer.builder()
        //.addCharFilter(SceneNameDividerCharFilterFactory::class.java)
        .addCharFilter(PatternReplaceCharFilterFactory.NAME, mapOf("pattern" to "-[a-fA-F0-9]{8}", "replacement" to "")) // scene hex replacement
        .addCharFilter(PatternReplaceCharFilterFactory.NAME, mapOf("pattern" to "[_\\.]", "replacement" to " "))
        .addCharFilter(PatternReplaceCharFilterFactory.NAME, mapOf("pattern" to "[\\(\\[\\)\\]]", "replacement" to ""))
        .addTokenFilter(LowerCaseFilterFactory.NAME)
        .addTokenFilter(TrimFilterFactory.NAME)
        .addTokenFilter(StopFileFilterFactory::class.java)
        .withTokenizer(StandardTokenizerFactory.NAME)
        .build()

    /**
     * Lucene special characters in the QUERY are *, ?, -, etc and need to be escaped
     */
    private val queryAnalyzer = CustomAnalyzer.builder()
        .addTokenFilter(LowerCaseFilterFactory.NAME)
        .addTokenFilter(TrimFilterFactory.NAME)
        .withTokenizer(StandardTokenizerFactory.NAME)
        .build()

    val wrapper = PerFieldAnalyzerWrapper(indexAnalyzer, mapOf(
        "artist" to indexAnalyzer,
        "album" to indexAnalyzer,
        "track" to indexAnalyzer,
    ))

    // TODO: Rework so that individual tracks can be grabbed from different search results according to best match
    fun matchResultToTrackList(results: List<SearchResult>, tracks: List<LidarrTrack>, albumName: String, artistName: String): SearchMatchResult {

        return results.map {
            val trackResultsForSearch = tracks.map { track -> findTrackInSlskd(it, track, albumName, artistName) }
            matchTracks(it, trackResultsForSearch )
        }
            .sortedByDescending { it.score }
            .first()

    }

    private fun matchTracks(result: SearchResult, trackResults: List<TrackMatchResult>): SearchMatchResult {
        var score = 10.0
        if (!result.hasFreeUploadSlot) {
            // small punishment for not *currently* having an upload slot
            score -= 1
        }

        // minimal score for upload speed
        score+= result.uploadSpeed / 100_000

        val relevantFiles = result.files.filter(this::musicFilter)
        score -= abs(relevantFiles.size - trackResults.size)

        // TODO: boost FLAC *properly*
        val isFlac = relevantFiles.filter { it.extension == "flac" || cleanupFilename(it.filename).fileName.endsWith(".flac") }.size >= (relevantFiles.size / 2.0)
                || trackResults.filter { it.file?.endsWith("flac") == true }.size >= (trackResults.size / 2.0)
        if (isFlac) {
            score += relevantFiles.size * 1.5
        }

        // TODO: add track match accuracy???
        for(trackResult in trackResults) {
            score += trackResult.score
        }


        // TODO: boost bitdepth

        //TODO: match runtime

        // https://github.com/guessit-io/guessit type for audio (part extraction)
        // beets match? https://github.com/beetbox/beets

        return SearchMatchResult(result, score, trackResults)

    }

    fun findTrackInSlskd(result: SearchResult, track: LidarrTrack, albumName: String, artistName: String): TrackMatchResult {
        val memoryIndex = ByteBuffersDirectory()
        val indexWriterConfig = IndexWriterConfig(wrapper)
        val writer = IndexWriter(memoryIndex, indexWriterConfig)

        for (file in result.files.filter(this::musicFilter)) {
            val info = extractInfo(file, albumName, artistName)
            val filename = cleanupFilename(file.filename).fileName.toString()
            val doc = Document()
            doc.add(Field("artist", info.artist ?: impossibleSearchResult.toString(), TYPE_STORED))
            doc.add(Field("album", info.album ?: impossibleSearchResult.toString(), TYPE_STORED))
            doc.add(Field("track", filename, TYPE_STORED))
            doc.add(Field("slskd-file", file.filename, TYPE_STORED))
            writer.addDocument(doc)
        }
        writer.close()

        // FIXME: Turn query into 2 steps - first an exact match on the track or "Artist - TrackName", then more error prone, but tolerant Lucene search
        // FIXME: improve query significantly, account for all fields, weigh in fuzzy, prefix and phrase queries for the track
        val parser = QueryParser("track", queryAnalyzer)
        val query = parser.parse(QueryParser.escape(track.title))

        val reader = DirectoryReader.open(memoryIndex)
        val searcher = IndexSearcher(reader)
        val storedFields = searcher.storedFields()

        val indexResult = searcher.search(query, 3).scoreDocs.map { storedFields.document(it.doc) }

        reader.close()
        memoryIndex.close()

        // FIXME: How do we know this is truly a reliable result?
        val score = when (indexResult.size) {
            1 -> 2 // one exact result => big bonus
            2, 3 -> 1 // some results, but not very clear? => small bonus
            0 -> -5 // no results -> punish via -5
            else -> -1
        }

        return TrackMatchResult(track, indexResult.firstOrNull()?.getField("slskd-file")?.stringValue(), score)
    }

    private fun musicFilter(file: SearchFile): Boolean {
        return file.extension == "mp3" || file.extension == "flac" || file.filename.endsWith(".mp3") || file.filename.endsWith(".flac")
    }

    private fun extractInfo(file: SearchFile, albumName: String, artistName: String): SearchResultInfo {
        val artistMatchName = artistName.lowercase()
        val albumMatchName = albumName.lowercase()

        val filePath = cleanupFilename(file.filename)
        // no need to turn recursive, there really shouldn't be more than 3 folders for structures like /music/artist/buffer-folder/album/track.mp3 or /music/artist/track.mp3
        val firstParent = filePath.parent
        val secondParent = firstParent.parent
        val thirdParent = secondParent.parent

        val firstParentFolderName = firstParent.fileName.toString().lowercase()
        val secondParentFolderName = secondParent.fileName.toString().lowercase()
        val thirdParentFolderName = thirdParent.fileName.toString().lowercase()

        val albumMatch = matches(listOf(firstParentFolderName, secondParentFolderName), albumMatchName, true)
        val artistMatch = matches(listOf(firstParentFolderName, secondParentFolderName, thirdParentFolderName), artistMatchName)

        return SearchResultInfo(albumMatch, artistMatch)
    }

    /**
     * Prepares filename for use with path API. This modifies the original path to be compliant with OS standards, e.g. Windows not allowing ? in filenames
     * Do NOT rely on this function if you intend to reference back to this filename. It will NOT match an existing one within Slskd anymore.
     */
    private fun cleanupFilename(filename: String): Path {
        val cleanName = filename
            .replace("@", "")
            .replace("?", "")
            .replace("\\", "/")
            .lowercase()
        return Path.of(cleanName)
    }

    /**
     * Used to see if an album or artist is represented by the given directory name.
     * If LevenshteinDistance is smaller than 20% of the total directory name, we assume it's a match.
     *
     * TODO: needs some better matching algorithm, cleanup for parts like [FLAC], WEB, etc
     * Maybe take into account how much longer the folder name is than the album name when calculating the 20-35% threshold
     */
    private fun matches(folders: List<String>, potentialMatch: String, removeYear: Boolean = false): String? {
        val lev = LevenshteinDistance.getDefaultInstance()
        for (folder in folders) {
            val folderName = if (removeYear) folder.replace(yearRegex, "") else folder
            val match = lev.apply(folderName, potentialMatch) < (folder.length * 0.35)
            if (match) {
                return folder
            }
        }
        return null
    }

    private data class SearchResultInfo(
        val album: String?,
        val artist: String?,
    )

}