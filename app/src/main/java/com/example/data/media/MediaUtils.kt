package com.example.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object MediaUtils {

    suspend fun compressUriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 720,
        maxBase64Chars: Int = 260_000
    ): String? = withContext(Dispatchers.IO) {
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val original = input.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
            val scale = minOf(
                maxDimension.toFloat() / original.width,
                maxDimension.toFloat() / original.height,
                1f
            )
            val targetW = (original.width * scale).toInt().coerceAtLeast(1)
            val targetH = (original.height * scale).toInt().coerceAtLeast(1)
            val scaled = if (targetW != original.width || targetH != original.height) {
                Bitmap.createScaledBitmap(original, targetW, targetH, true)
            } else {
                original
            }

            var quality = 76
            var base64: String
            do {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                quality -= 12
            } while (base64.length > maxBase64Chars && quality >= 28)

            if (scaled != original) scaled.recycle()
            if (base64.length <= maxBase64Chars) base64 else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun decodeUriToScaledBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1024
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val original = input.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
            val scale = minOf(
                maxDimension.toFloat() / original.width,
                maxDimension.toFloat() / original.height,
                1f
            )
            val targetW = (original.width * scale).toInt().coerceAtLeast(1)
            val targetH = (original.height * scale).toInt().coerceAtLeast(1)
            if (targetW != original.width || targetH != original.height) {
                val scaled = Bitmap.createScaledBitmap(original, targetW, targetH, true)
                original.recycle()
                scaled
            } else {
                original
            }
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val clean = base64.substringAfter("base64,", base64)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    fun getFileDisplayName(context: Context, uri: Uri): String {
        var name = "Document"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(idx) ?: name
            }
        }
        return name.take(100)
    }

    suspend fun readSmallFileBase64(context: Context, uri: Uri, maxBytes: Int = 220_000): String? =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    if (bytes.size <= maxBytes) Base64.encodeToString(bytes, Base64.NO_WRAP) else null
                }
            } catch (_: Exception) {
                null
            }
        }

    suspend fun saveBase64ImageToGallery(context: Context, base64: String, fileNamePrefix: String = "WeConnect"): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val clean = base64.substringAfter("base64,", base64)
                val bytes = Base64.decode(clean, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext false

                val filename = "${fileNamePrefix}_${System.currentTimeMillis()}.jpg"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/WeConnect")
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        ?: return@withContext false
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    } ?: return@withContext false
                    true
                } else {
                    val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                    val dir = File(picturesDir, "WeConnect")
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, filename)
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    true
                }
            } catch (_: Exception) {
                false
            }
        }

    suspend fun saveBase64FileToDownloads(
        context: Context,
        base64: String,
        fileName: String,
        mimeType: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val clean = base64.substringAfter("base64,", base64)
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            val safeName = fileName.trim().ifBlank { "WeConnect_${System.currentTimeMillis()}.bin" }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType ?: "application/octet-stream")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/WeConnect")
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return@withContext false
                true
            } else {
                val dlDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val dir = File(dlDir, "WeConnect")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, safeName)
                FileOutputStream(file).use { it.write(bytes) }
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun decodeFileToImageBitmap(filePath: String?): ImageBitmap? {
        if (filePath.isNullOrBlank()) return null
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}

class VoiceNoteRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(): Boolean {
        return try {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            outputFile = file
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(32000)
            mr.setAudioSamplingRate(22050)
            mr.setOutputFile(file.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            true
        } catch (_: Exception) {
            release()
            false
        }
    }

    fun stopAndEncodeBase64(): String? {
        return try {
            recorder?.stop()
            val file = outputFile
            release()
            if (file != null && file.exists()) {
                val bytes = file.readBytes()
                file.delete()
                if (bytes.isNotEmpty() && bytes.size <= 280_000) {
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                } else null
            } else null
        } catch (_: Exception) {
            release()
            null
        }
    }

    fun release() {
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
    }
}

object VoiceNotePlayer {
    private var player: MediaPlayer? = null

    fun playBase64(context: Context, base64Audio: String, onComplete: () -> Unit) {
        stop()
        try {
            val bytes = Base64.decode(base64Audio, Base64.NO_WRAP)
            val tempFile = File(context.cacheDir, "play_${System.currentTimeMillis()}.m4a")
            FileOutputStream(tempFile).use { it.write(bytes) }
            val mp = MediaPlayer()
            mp.setDataSource(tempFile.absolutePath)
            mp.setOnCompletionListener {
                tempFile.delete()
                stop()
                onComplete()
            }
            mp.setOnErrorListener { _, _, _ ->
                tempFile.delete()
                stop()
                onComplete()
                true
            }
            mp.prepare()
            mp.start()
            player = mp
        } catch (_: Exception) {
            stop()
            onComplete()
        }
    }

    fun stop() {
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {
        }
        player = null
    }
}
