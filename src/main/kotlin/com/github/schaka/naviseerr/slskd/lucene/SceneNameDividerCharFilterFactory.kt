package com.github.schaka.naviseerr.slskd.lucene

import org.apache.lucene.analysis.CharFilterFactory
import org.apache.lucene.analysis.charfilter.BaseCharFilter
import java.io.IOException
import java.io.Reader
import java.io.StringReader
import kotlin.math.round

/**
 * Replaces the divider of 01_artist-songname_here.flac type naming (mostly scene releases)
 * We don't want to replace dashes that are potentially intentional in naming.
 */
class SceneNameDividerCharFilterFactory(map: Map<String, String>) : CharFilterFactory(map) {

    override fun create(input: Reader?): Reader {
        return SceneNameDividerCharFilter(input)
    }

    override fun normalize(input: Reader?): Reader {
        return create(input)
    }

    private class SceneNameDividerCharFilter(input: Reader?): BaseCharFilter(input) {

        private var transformedInput: Reader? = null

        @Throws(IOException::class)
        private fun fill() {
            val buffered = StringBuilder()
            val temp = CharArray(1024)
            var cnt = input.read(temp)
            while (cnt > 0) {
                buffered.appendRange(temp, 0, cnt)
                cnt = input.read(temp)
            }
            transformedInput = StringReader(processPattern(buffered))
        }

        override fun read(cbuf: CharArray, off: Int, len: Int): Int {

            // Buffer all input on the first call.
            if (transformedInput == null) {
                fill()
            }

            return transformedInput!!.read(cbuf, off, len)
        }

        private fun processPattern(buffer: StringBuilder): String {
            val given = buffer.toString()
            if (given.contains(" ")) {
                // not a scene name, return as is
                return given
            }

            // uneven, use center-most occurrence
            val occurrences = given.count{ it == '-' }
            if (occurrences % 2 != 0) {
                val targetOccurrence = round(occurrences / 2.0).toInt()
                val targetIndex = targetIndex(given, targetOccurrence)
                val newString = given.replaceRange(targetIndex, targetIndex + 1, " - ")
                val lengthBeforeReplacement = given.length
                val replacementSize = 3
                //addOffCorrectMap()

                return newString
            }

            // even amount
            // TODO: this needs to be smarter, somehow?
            return given.replace("-", " ")
        }

        private fun targetIndex(given: String, targetOccurrence: Int): Int {
            var match = 0
            for (i in 0..given.length) {
                if (given[i] == '-') {
                    match++
                }
                if (match == targetOccurrence) {
                    return i
                }
            }
            return 0
        }
    }

}