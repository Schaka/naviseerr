package com.github.schaka.naviseerr.download_client.slskd.lucene

import org.apache.lucene.analysis.CharArraySet
import org.apache.lucene.analysis.core.StopFilterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class StopFileFilterFactory(map: Map<String, String>,
) : StopFilterFactory(map) {

    private val words: CharArraySet

    init {
        StopFilterFactory::class.java.classLoader.getResourceAsStream("lucene/stopwords.txt").use {
            val inputStreamReader = InputStreamReader(it, StandardCharsets.UTF_8)
            val bufferedReader = BufferedReader(inputStreamReader)
            val wordsList = bufferedReader.lines().toList()
            words = CharArraySet(wordsList, true)
        }
    }

    override fun createDefaultWords(): CharArraySet {
        return words
    }


}