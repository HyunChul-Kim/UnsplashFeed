package com.hyunchul.unsplashfeed.data.local.file

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import java.io.File
import java.io.OutputStream

class PhotoFileStore(
    private val filesDir: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend fun save(
        writeContent: suspend (OutputStream) -> Unit,
    ): String {
        var createdFile: File? = null

        try {
            return withContext(ioDispatcher) {
                val directory = File(filesDir, DIRECTORY_NAME)
                if(
                    !directory.isDirectory &&
                    !directory.mkdirs() &&
                    !directory.isDirectory
                ) {
                    throw IOException("Cannot create favorites directory.")
                }

                val file = File.createTempFile("photo_", ".img", directory)
                createdFile = file

                file.outputStream().buffered().use { output ->
                    writeContent(output)
                }
                ensureActive()

                if(file.length() == 0L) {
                    throw IOException("No photo bytes were written.")
                }

                "$DIRECTORY_NAME/${file.name}"
            }
        } catch(failure: Throwable) {
            createdFile?.let { file ->
                try {
                    withContext(NonCancellable) {
                        withContext(ioDispatcher) {
                            deleteFileIfPresent(file)
                        }
                    }
                } catch(cleanupFailure: Throwable) {
                    if(cleanupFailure !== failure) {
                        failure.addSuppressed(cleanupFailure)
                    }
                }
            }

            throw failure
        }
    }

    fun fileFor(relativePath: String): File {
        require(RELATIVE_PATH_PATTERN.matches(relativePath)) {
            "Expected a file path directly under favorites/"
        }
        return File(filesDir, relativePath)
    }

    suspend fun delete(relativePath: String) {
        val file = fileFor(relativePath)
        withContext(ioDispatcher) {
            deleteFileIfPresent(file)
        }
    }

    private fun deleteFileIfPresent(file: File) {
        if(file.isDirectory) {
            throw IOException("Refusing to delete a directory: ${file.name}")
        }
        if(!file.delete() && file.exists()) {
            throw IOException("Cannot delete file: ${file.name}")
        }
    }

    private companion object {
        const val DIRECTORY_NAME = "favorites"

        val RELATIVE_PATH_PATTERN = Regex("""favorites/[A-Za-z0-9_-]+(?:\.[A-Za-z0-9_-]+)*""")
    }
}