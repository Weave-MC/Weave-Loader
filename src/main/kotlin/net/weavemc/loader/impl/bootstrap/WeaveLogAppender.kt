package net.weavemc.loader.impl.bootstrap

import me.xtrm.klog.Appender
import me.xtrm.klog.LogContext
import me.xtrm.klog.dsl.klog
import net.weavemc.loader.impl.util.getOrCreateDirectory
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

/**
 * We have to do a bit of gymnastics to ensure we don't reinitialize our logfile, create two logfiles,
 * or overwrite stuff we don't want to.
 *
 * Since this class is loaded on two different classloaders, we need to pass it some state (here `initialized`)
 * to determine whether start a new logging session or continue an existing one.
 *
 * @param initialized Whether the logger has already been initialized
 */
object WeaveLogAppender : Appender {
    private val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss")
    private val logDir = getOrCreateDirectory("logs")
    private val logFile: Path

    private val stdoutStream = WrappingStream(System.out)
    private val stderrStream = WrappingStream(System.err)
    private val logFileStream: PrintStream

    private val logger by klog

    init {
        logFile = fetchLogFile(false)
        logFileStream = PrintStream(FileOutputStream(logFile.toFile(), false), true)
        symlinkLatest()
    }

    override fun append(context: LogContext, finalMessage: String) {
        stdoutStream.println(finalMessage)
        stdoutStream.flush()
        logFileStream.println(finalMessage)
        logFileStream.flush()

        val args = context.args
        if (args.isNotEmpty()) {
            val last = args.last()
            if (last is Throwable) {
                last.printStackTrace(stderrStream)
                last.printStackTrace(logFileStream)
            }
        }
    }

    private fun symlinkLatest() {
        val latest = logDir.resolve("latest.log")
        latest.deleteIfExists()
        runCatching {
            Files.createLink(latest, logFile)
        }.onFailure { e1 ->
            runCatching {
                Files.createSymbolicLink(latest, logFile)
            }.onFailure { e2 ->
                e1.addSuppressed(e2)
                logger.error("Failed to create (sym)link to latest log", e1)
            }
        }
    }

    private fun fetchLogFile(initialized: Boolean): Path {
        val latestLog = logDir.resolve("latest.log")
        if (!initialized || !latestLog.exists()) {
            return logDir.resolve("weave-loader-${LocalDateTime.now().format(formatter)}.log")
        }
        // If the loader is already initialized, we will just write to the latest log
        return latestLog
    }

    internal class WrappingStream(private val stream: PrintStream) : PrintStream(stream) {
        override fun println(x: Any?) {
            // Use print with a manual newline to prevent Legacy Forge from redirecting to log4j
            stream.print("$x\n")
        }
    }
}