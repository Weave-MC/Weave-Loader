package net.weavemc.loader.mappings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.weavemc.loader.DownloadUtil
import net.weavemc.loader.JSON
import net.weavemc.loader.ModCachingManager
import net.weavemc.weave.api.gameVersion
import java.io.File
import java.io.InputStream
import java.net.URL

object MappingsManager {

    private inline fun <reified T> String?.decodeJSON() =
        if (this != null) JSON.decodeFromString<T>(this) else null

    /**
     * Retrieves a text file containing srg, yarn, and mojmap mappings
     */
    fun getOrCreateBundledMappings(): File? {
        val bundledFile = ModCachingManager.cacheDirectory
            .resolve("mappings")
            .resolve(gameVersion.versionName)
            .resolve("bundled.mappings").toFile()

        if (bundledFile.exists())
            return bundledFile

        bundledFile.parentFile.mkdirs()

        val srgStream = MappingsManager.javaClass.getResourceAsStream(
            "weave/mappings/srg/${gameVersion.aliases[0]}"
        ) ?: error("Could not retrieve SRG mappings for ${gameVersion.aliases[0]}")
        val yarnStream = MappingsManager.javaClass.getResourceAsStream(
            "weave/mappings/yarn/${gameVersion.aliases[0]}"
        ) ?: error("Could not retrieve Yarn mappings for ${gameVersion.aliases[0]}")

        bundledFile.writeBytes(srgStream.readBytes())
        bundledFile.appendBytes(yarnStream.readBytes())

        val mojmapFile = downloadMojangMappings()
        if (mojmapFile != null)
            bundledFile.appendBytes(mojmapFile.readBytes())

        return bundledFile
    }

    private fun InputStream.toLines(): List<String> =
        this.readBytes().decodeToString().trim().lines()

    private fun downloadMojangMappings(): File? {
        val manifest = DownloadUtil.fetch(
            "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"
        ).decodeJSON<VersionManifest>() ?: return null

        val versionEntry = manifest.versions.find { it.id == gameVersion.versionName } ?: return null
        val versionInfo = DownloadUtil.fetch(versionEntry.url).decodeJSON<VersionInfo>() ?: return null
        val mappings = versionInfo.downloads.mappings

        if (mappings.size != -1) {
            val path = ModCachingManager.cacheDirectory
                .resolve("mappings")
                .resolve(gameVersion.versionName)
                .resolve("mojang.mappings")

            DownloadUtil.checksumAndDownload(
                URL(mappings.url),
                mappings.sha1,
                path
            )

            return path.toFile()
        }

        return null
    }

    @Serializable
    private data class VersionManifest(val versions: List<ManifestVersion>)

    @Serializable
    private data class ManifestVersion(val id: String, val url: String)

    @Serializable
    private data class VersionInfo(val downloads: VersionDownloads, val libraries: List<Library>)

    @Serializable
    private data class VersionDownloads(
        val client: VersionDownload,
        @SerialName("client_mappings") val mappings: ClientMappings // not all versions will have a client_mappings url
    )

    @Serializable
    private data class ClientMappings(
        val url: String,
        val sha1: String,
        val size: Int = -1
    )

    @Serializable
    private data class VersionDownload(val url: String, val sha1: String)

    @Serializable
    private data class Library(val name: String)
}