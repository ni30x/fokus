package nwd.fokuslauncher.utils

import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Builds a plain-text diagnostic file (device / app metadata + logcat) for user-initiated export
 * from Settings.
 *
 * Captures the **crash** log buffer and the app's UID-scoped **main** buffer without `--pid`, so
 * lines from earlier process instances (e.g. right before a crash) remain when the user reopens
 * the app and exports.
 */
object AppDiagnosticLogExporter {

    private const val MAIN_LOG_MAX_LINES = "20000"
    private const val CRASH_LOG_MAX_LINES = "2000"

    fun writeExportFile(context: Context): File {
        val dir = File(context.cacheDir, "log_export").apply { mkdirs() }
        val outFile = File(dir, "fokus-launcher-diagnostic.txt")
        val header = buildHeader(context)
        val logs = captureDiagnosticLogs()
        outFile.writeText(header + logs)
        return outFile
    }

    private fun buildHeader(context: Context): String {
        val pm = context.packageManager
        val pkg = context.packageName
        val pInfo = pm.getPackageInfo(pkg, 0)
        val versionName = pInfo.versionName ?: "?"
        val versionCode = PackageInfoCompat.getLongVersionCode(pInfo)
        return buildString {
            appendLine("Fokus Launcher — diagnostic export")
            appendLine("Generated: ${OffsetDateTime.now(ZoneId.systemDefault())}")
            appendLine("Package: $pkg")
            appendLine("Version: $versionName ($versionCode)")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("SDK: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
            appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("Process ID (this export): ${android.os.Process.myPid()}")
            appendLine(
                    "Note: Log sections include prior app sessions (other PIDs) when still in the " +
                            "system logcat buffer."
            )
            appendLine("=".repeat(60))
            appendLine()
        }
    }

    private fun captureDiagnosticLogs(): String =
            buildString {
                appendSection("CRASH LOG BUFFER") {
                    append(
                            captureLogcat(
                                    listOf(
                                            "logcat",
                                            "-d",
                                            "-b",
                                            "crash",
                                            "-v",
                                            "threadtime",
                                            "-t",
                                            CRASH_LOG_MAX_LINES,
                                    ),
                                    emptyMessage = "(No lines in crash buffer.)\n",
                            )
                    )
                }
                appendSection("APP LOG (all recent sessions for this app)") {
                    append(
                            captureLogcat(
                                    listOf(
                                            "logcat",
                                            "-d",
                                            "-v",
                                            "threadtime",
                                            "-t",
                                            MAIN_LOG_MAX_LINES,
                                    ),
                                    emptyMessage = "(No log lines returned for this app.)\n",
                            )
                    )
                }
            }

    private fun StringBuilder.appendSection(title: String, block: StringBuilder.() -> Unit) {
        appendLine("-".repeat(60))
        appendLine(title)
        appendLine("-".repeat(60))
        block()
        appendLine()
    }

    private fun captureLogcat(args: List<String>, emptyMessage: String): String =
            runCatching {
                        val process = ProcessBuilder(args).redirectErrorStream(true).start()
                        val text =
                                process.inputStream.bufferedReader(Charsets.UTF_8).use {
                                    it.readText()
                                }
                        val exit = process.waitFor()
                        when {
                            exit != 0 && text.isBlank() ->
                                    "(logcat exited with code $exit and no output)\n"
                            text.isBlank() -> emptyMessage
                            else -> if (text.endsWith('\n')) text else "$text\n"
                        }
                    }
                    .getOrElse { e ->
                        "(Log capture failed: ${e.javaClass.simpleName}: ${e.message})\n"
                    }
}
