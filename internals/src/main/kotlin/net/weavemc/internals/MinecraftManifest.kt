package net.weavemc.internals

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private inline fun <reified T> String?.decodeJSON() =
    if (this != null) manifestDecoder.decodeFromString<T>(this) else null

val manifestDecoder = Json { ignoreUnknownKeys = true }
const val minecraftManifestURL = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"

fun fetchVersionManifest() = DownloadUtil.fetch(minecraftManifestURL).decodeJSON<VersionManifest>()
fun VersionManifest.fetchVersion(id: String) = versions.find { it.id == id }?.fetch()
fun ManifestVersion.fetch() = DownloadUtil.fetch(url).decodeJSON<VersionInfo>()

@Serializable
data class VersionManifest(val versions: List<ManifestVersion>)

@Serializable
data class ManifestVersion(val id: String, val url: String)

@Serializable
data class VersionInfo(
    val downloads: VersionDownloads,
    val libraries: List<Library>,
    val javaVersion: MinecraftJavaVersion,
    val id: String,
    val assetIndex: AssetIndex,
    val mainClass: String,
    val logging: LoggingConfiguration
)

@Serializable
data class LoggingConfiguration(
    val client: SideLoggingConfiguration,
    val server: SideLoggingConfiguration? = null
)

@Serializable
data class SideLoggingConfiguration(
    val argument: String,
    val file: LoggingFile,
    val type: String
)

@Serializable
data class LoggingFile(
    val id: String,
    val sha1: String,
    val size: Int,
    val url: String
)

@Serializable
data class AssetIndex(val id: String)

@Serializable
data class MinecraftJavaVersion(val component: String?, val majorVersion: Int)

@Serializable
data class VersionDownloads(val client: VersionDownload)

@Serializable
data class VersionDownload(val url: String, val sha1: String)

val VersionInfo.relevantLibraries get() = libraries.filter { lib -> lib.rules?.all { it.matches } ?: true }

@Serializable
data class Library(
    val downloads: LibraryDownloads,
    val name: String,
    val rules: List<OSRule>? = null
)

@Serializable
data class LibraryDownloads(
    val artifact: LibraryArtifact? = null,
    val classifiers: Map<String, LibraryArtifact>? = null
)

val LibraryDownloads.natives get() = classifiers?.get("natives-${osIdentifier()}")
val LibraryDownloads.allDownloads get() = listOfNotNull(artifact) + listOfNotNull(natives)

@Serializable
data class LibraryArtifact(
    val path: String,
    val sha1: String,
    val size: Int,
    val url: String
)

@Serializable
data class OSRule(
    val action: String,
    val os: OSInfo? = null,
)

val OSRule.allow
    get() = when (action) {
        "allow" -> true
        "disallow" -> false
        else -> error("Invalid action $action")
    }

val OSRule.matches get() = os == null || allow xor (os.name != osIdentifier())

@Serializable
data class OSInfo(
    val name: String? = null,
    val version: String? = null,
    val arch: String? = null,
)

fun osIdentifier() = with(System.getProperty("os.name")) {
    when {
        startsWith("Mac OS") -> "osx"
        startsWith("Linux") -> "linux"
        startsWith("Windows") -> "windows"
        else -> error("Unsupported platform ($this) (for minecraft OS identification)")
    }
}