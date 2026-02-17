package com.github.schaka.naviseerr.slskd

import com.github.schaka.naviseerr.slskd.dto.SearchFile
import com.github.schaka.naviseerr.slskd.dto.SearchMatchResult
import com.github.schaka.naviseerr.slskd.dto.SearchResult
import com.github.schaka.naviseerr.slskd.dto.TrackMatchResult
import com.github.schaka.naviseerr.slskd.lucene.StopFileFilterFactory
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
import org.apache.lucene.document.Field.Store
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField.TYPE_STORED
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.queryparser.classic.QueryParser.escape
import org.apache.lucene.search.*
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.store.ByteBuffersDirectory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.util.*
import kotlin.collections.map
import kotlin.math.abs

@Service
class MatchService {

    private val log = LoggerFactory.getLogger(javaClass)

    private val yearRegex = Regex("\\s?[\\[(]\\d{4}[)\\]]")
    // this needs to be a fallback value we can always find again, but not a string that can be randomly contained in a song name
    private val impossibleSearchResult: UUID = UUID.randomUUID()

    private val indexAnalyzer = CustomAnalyzer.builder()
        //.addCharFilter(SceneNameDividerCharFilterFactory::class.java)
        .addCharFilter(PatternReplaceCharFilterFactory::class.java, mapOf("pattern" to "-[a-fA-F0-9]{8}", "replacement" to "")) // scene hex replacement
        .addCharFilter(PatternReplaceCharFilterFactory::class.java, mapOf("pattern" to "[_\\.]", "replacement" to " "))
        .addCharFilter(PatternReplaceCharFilterFactory::class.java, mapOf("pattern" to "[\\(\\[\\)\\]]", "replacement" to ""))
        .addTokenFilter(LowerCaseFilterFactory::class.java)
        .addTokenFilter(TrimFilterFactory::class.java)
        .addTokenFilter(StopFileFilterFactory::class.java)
        .withTokenizer(StandardTokenizerFactory::class.java)
        .build()

    /**
     * Lucene special characters in the QUERY are *, ?, -, etc and need to be escaped
     */
    private val queryAnalyzer = CustomAnalyzer.builder()
        .addTokenFilter(LowerCaseFilterFactory::class.java)
        .addTokenFilter(TrimFilterFactory::class.java)
        .withTokenizer(StandardTokenizerFactory::class.java)
        .build()

    val wrapper = PerFieldAnalyzerWrapper(indexAnalyzer, mapOf(
        "artist" to indexAnalyzer,
        "album" to indexAnalyzer,
        "track" to indexAnalyzer,
    ))

    // TODO: Rework so that individual tracks can be grabbed from different search results according to best match
    fun matchResultToTrackList(results: List<SearchResult>, tracks: List<LidarrTrack>, albumName: String, artistName: String): List<SearchMatchResult> {

        return results.map {
            val trackResultsForSearch = tracks.map { track -> findTrackInSlskd(it, track, albumName, artistName) }
            matchTracks(it, trackResultsForSearch )
        }

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

        val isFlac = relevantFiles.filter { it.extension == "flac" || cleanupFilename(it.filename).fileName.endsWith(".flac") }.size >= (relevantFiles.size / 2.0)
                || trackResults.filter { it.file?.filename?.endsWith("flac") == true }.size >= (trackResults.size / 2.0)
        if (isFlac) {
            score += relevantFiles.size * 1.5
        }

        // TODO: add track match accuracy???
        for(trackResult in trackResults) {
            score += trackResult.score
        }

        // boosting bitDepth
        relevantFiles.filter { it.bitDepth == 24 }.forEach { score+= 1.0 / relevantFiles.size }

        trackResults.forEach {
            val expectedRunTime = it.lidarrTrack.duration
            val runTime = it.file?.length ?: 0

            if (it.file != null && musicFilter(it.file) && expectedRunTime > 0 && runTime > 0
                && abs(expectedRunTime - runTime) <= expectedRunTime * 0.1) {
                // within 10% of each other's runtime
                score+= 1.0 / relevantFiles.size
            }
        }


        // Better matching via Artist + Album + Track, maybe?
        // https://github.com/guessit-io/guessit type for audio (part extraction)
        // beets match? https://github.com/beetbox/beets
        return SearchMatchResult(result, score, trackResults)

    }

    private fun findTrackInSlskd(result: SearchResult, track: LidarrTrack, albumName: String, artistName: String): TrackMatchResult {
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
            // store other data necessary
            doc.add(StringField("filename", file.filename, Store.YES))
            writer.addDocument(doc)
        }
        writer.close()

        // TODO: Turn query into 2 steps - first an exact match on "TrackName" or "Artist - TrackName", then more error prone, but tolerant Lucene search
        val reader = DirectoryReader.open(memoryIndex)
        val searcher = IndexSearcher(reader)
        val storedFields = searcher.storedFields()

        val combinedQuery = buildQuery(track, albumName, artistName)

        val indexResult = searcher.search(combinedQuery, 3).scoreDocs.map { storedFields.document(it.doc) }

        reader.close()
        memoryIndex.close()

        // TODO: maybe find a better way to score a file result - can't use Lucene score though
        val score = when (indexResult.size) {
            1 -> 2 // one exact result => big bonus
            2, 3 -> 1 // some results, but not very clear? => small bonus
            0 -> -5 // no results -> punish via -5
            else -> -1
        }

        val match = indexResult.firstOrNull()
        val filename = match?.getField("filename")?.stringValue()
        // some ID would obviously be better, but the filename that Slskd return is BASICALLY unique
        val searchFile = result.files.find { it.filename == filename }


        // TODO: return null on no match?
        log.trace ("Search for $artistName (Artist) $albumName (Album) - found: $searchFile" )
        return TrackMatchResult(track, score, searchFile)
    }

    private fun buildQuery(track: LidarrTrack, artistName: String, albumName: String): BooleanQuery {
        val parser = QueryParser("track", queryAnalyzer)

        val trackName = escape(track.title)
        val trackAndArtistName = escape("$artistName - ${track.title}")

        val trackTerm = Term("track", queryAnalyzer.normalize("track", trackName))
        val trackAndArtistTerm = Term("track", queryAnalyzer.normalize("track", trackAndArtistName))

        val artistPhraseQuery = parser.createPhraseQuery("artist", artistName, 1)
        val artistTermQuery = parser.parse(artistName)

        val artistQuery = BooleanQuery.Builder()
            .add(BoostQuery(artistPhraseQuery, 1.5F), Occur.SHOULD)
            .add(BoostQuery(artistTermQuery, 1.0F), Occur.MUST)
            .build()

        val albumPhraseQuery = parser.createPhraseQuery("album", albumName, 1)
        val albumTermQuery = parser.parse(albumName)

        val albumQuery = BooleanQuery.Builder()
            .add(BoostQuery(albumPhraseQuery, 1.5F), Occur.SHOULD)
            .add(BoostQuery(albumTermQuery, 1.0F), Occur.MUST)
            .build()

        val prefixQuery = BooleanQuery.Builder()
            .add(PrefixQuery(trackTerm), Occur.SHOULD)
            .add(PrefixQuery(trackAndArtistTerm), Occur.SHOULD)
            .build()

        // trackTerm uses whole string, parser tokenizes
        val termQuery = BooleanQuery.Builder()
            .add(parser.parse(trackName), Occur.MUST)
            .add(parser.parse(trackAndArtistName), Occur.SHOULD)
            .build()

        // this essentially checks word order
        val phraseQuery = parser.createPhraseQuery("track", trackName, 2)

        // fuzzy match with lower priority
        //TODO: guarantee that fuzzyQuery is built from Analyzer/QueryParser
        val fuzzyQuery = BooleanQuery.Builder()
            .add(FuzzyQuery(trackTerm, 2), Occur.MUST)
            .add(FuzzyQuery(trackAndArtistTerm, 2), Occur.SHOULD)
            .build()

        val trackQuery = BooleanQuery.Builder()
            .add(BoostQuery(prefixQuery, 1.15F), Occur.SHOULD)
            .add(BoostQuery(termQuery, 1.0F), Occur.MUST)
            .add(BoostQuery(phraseQuery, 1.5F), Occur.SHOULD)
            .add(BoostQuery(fuzzyQuery, 0.65F), Occur.SHOULD)
            .build()

        return BooleanQuery.Builder()
            .add(artistQuery, Occur.SHOULD)
            .add(albumQuery, Occur.SHOULD)
            .add(trackQuery, Occur.MUST)
            .build()
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
        val secondParent = firstParent?.parent
        val thirdParent = secondParent?.parent

        val firstParentFolderName = firstParent.fileName.toString().lowercase()
        val secondParentFolderName = secondParent?.fileName.toString().lowercase()
        val thirdParentFolderName = thirdParent?.fileName.toString().lowercase()

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
     * If LevenshteinDistance is smaller than 30% of the total directory name, we assume it's a match.
     *
     * TODO: needs some better matching algorithm, cleanup for parts like [FLAC], WEB, etc
     * Maybe take into account how much longer the folder name is than the album name when calculating the threshold
     */
    private fun matches(folders: List<String>, potentialMatch: String, removeYear: Boolean = false): String? {
        val lev = LevenshteinDistance.getDefaultInstance()
        for (folder in folders) {
            val folderName = if (removeYear) folder.replace(yearRegex, "") else folder
            val match = lev.apply(folderName, potentialMatch) < (folder.length * 0.30)
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