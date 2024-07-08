package net.weavemc.loader.impl.bootstrap

import me.xtrm.klog.Appender
import me.xtrm.klog.LogContext
import me.xtrm.klog.dsl.klog
import net.weavemc.loader.impl.util.getOrCreateDirectory
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.deleteIfExists

/**
 * We have to do a bit of gymnastics to ensure we don't reinitialize our logfile, create two logfiles,
 * or overwrite stuff we don't want to.
 *
 * Since this class is loaded on two different classloaders, we need to pass it some state (here `initialized`)
 * to determine whether to start a new logging session or to continue an existing one.
 */
internal object WeaveLogAppender : Appender {
    private val newline = System.lineSeparator()
    private val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss")
    private val logDir = getOrCreateDirectory("logs")
    private val logFile = logDir.resolve("weave-loader-${LocalDateTime.now().format(formatter)}.log")

    private val stdoutStream = WrappingStream(System.out)
    private val stderrStream = WrappingStream(System.err)
    private val logFileStream = PrintStream(FileOutputStream(logFile.toFile(), true), true)

    private val logger by klog

    init {
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

    internal class WrappingStream(private val stream: PrintStream) : PrintStream(stream) {
        override fun println(x: Any?) {
            // Use print with a manual newline to prevent Legacy Forge from redirecting to log4j
            stream.print(x.toString() + newline)
        }
    }
}