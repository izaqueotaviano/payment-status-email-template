package com.iptvtv.player.data.remote

import okhttp3.Interceptor
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import okio.buffer

/** Report download progress at most every 256 KB, so the callback never paces the download. */
private const val REPORT_EVERY_BYTES = 256L * 1024

/**
 * Attached to a request whose download should be measured. [onBytes] receives how much has arrived
 * and how much the server declared, 0 when it declared nothing.
 */
class ImportProgressTag(val onBytes: (bytesRead: Long, totalBytes: Long) -> Unit)

/**
 * Counts a download where the numbers are still true.
 *
 * This has to be a *network* interceptor. OkHttp asks for gzip on its own and, when the server
 * obliges, removes `Content-Length` and hands the layers above a decompressed stream of unknown
 * length - so measuring up there yields no percentage at all, which is exactly the sweeping bar
 * this was supposed to replace. Down here the raw header is still present and the bytes counted are
 * the bytes actually transferred.
 */
val importProgressInterceptor = Interceptor { chain ->
    val request = chain.request()
    val response = chain.proceed(request)
    val tag = request.tag(ImportProgressTag::class.java) ?: return@Interceptor response
    val body = response.body ?: return@Interceptor response

    val declared = response.header("Content-Length")?.toLongOrNull() ?: -1L
    val total = declared.coerceAtLeast(0L)
    val counting = CountingSource(body.source()) { read -> tag.onBytes(read, total) }
    response.newBuilder()
        .body(counting.buffer().asResponseBody(body.contentType(), declared))
        .build()
}

/**
 * Counts bytes as they are read, reporting at most every [REPORT_EVERY_BYTES] plus once at the end
 * so the bar reaches where it actually got to instead of stopping just short.
 */
class CountingSource(
    delegate: Source,
    private val onProgress: (Long) -> Unit,
) : ForwardingSource(delegate) {
    private var totalRead = 0L
    private var lastReported = 0L

    override fun read(sink: Buffer, byteCount: Long): Long {
        val read = super.read(sink, byteCount)
        if (read == -1L) {
            if (totalRead != lastReported) {
                lastReported = totalRead
                onProgress(totalRead)
            }
            return read
        }
        totalRead += read
        if (totalRead - lastReported >= REPORT_EVERY_BYTES) {
            lastReported = totalRead
            onProgress(totalRead)
        }
        return read
    }
}
