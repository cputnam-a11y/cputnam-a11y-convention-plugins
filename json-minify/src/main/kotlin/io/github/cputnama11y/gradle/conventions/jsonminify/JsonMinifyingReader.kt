package io.github.cputnama11y.gradle.conventions.jsonminify

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.io.FilterReader
import java.io.IOException
import java.io.Reader
import java.io.StringReader

class JsonMinifyingReader(`in`: Reader) : FilterReader(`in`) {
    var tracker : JsonStatsTracker? = null
    private val delegate: StringReader by lazy {
        val start = System.currentTimeMillis()
        var bytesSaved = 0L
        try {
            val json = StringBuilder()
            val buffer = CharArray(8192)
            var n: Int

            while ((`in`.read(buffer).also { n = it }) != -1) {
                json.appendRange(buffer, 0, n)
            }
            val oldString = json.toString().encodeToByteArray()
            val minified: String = try {
                JsonOutput.toJson(JsonSlurper().parse(oldString))
            } catch (e: Exception) {
                println(e.message)
                println(e.stackTrace.contentToString())
                throw IOException("Failed to minify JSON", e)
            }
            bytesSaved = (oldString.size - minified.encodeToByteArray().size).toLong()

            StringReader(minified)
        } finally {
            val end = System.currentTimeMillis()
            tracker?.jsonBytesSaved += bytesSaved
            tracker?.jsonTotalTime = end - start
            tracker?.jsonMinified++
        }
    }

    @Throws(IOException::class)
    override fun read(): Int {
        return delegate.read()
    }

    @Throws(IOException::class)
    override fun read(cbuf: CharArray, off: Int, len: Int): Int {
        return delegate.read(cbuf, off, len)
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        return delegate.skip(n)
    }

    @Throws(IOException::class)
    override fun ready(): Boolean {
        return delegate.ready()
    }

    @Throws(IOException::class)
    override fun close() {
        delegate.close()
        super.close()
    }

    override fun markSupported(): Boolean {
        return delegate.markSupported()
    }

    @Throws(IOException::class)
    override fun mark(readAheadLimit: Int) {
        delegate.mark(readAheadLimit)
    }

    @Throws(IOException::class)
    override fun reset() {
        delegate.reset()
    }
}