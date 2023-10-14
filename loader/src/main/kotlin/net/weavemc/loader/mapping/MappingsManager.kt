package net.weavemc.loader.mapping

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.weavemc.loader.DownloadUtil
import net.weavemc.loader.JSON
import net.weavemc.loader.FileManager
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
    fun getOrCreateBundledMappings(): File {
        val bundledFile = FileManager.CACHE_DIRECTORY
            .resolve("mappings")
            .resolve(gameVersion.versionName)
            .resolve("bundled.mappings").toFile()

        // TODO add a way to tell what mappings are bundled in this file
        // TODO if the bundledfile match the mappings weave loader is attempting to bundle, return the cached file
        if (bundledFile.exists())
            return bundledFile

        bundledFile.parentFile.mkdirs()

        var failCount: Int = 0

        val srgStream = MappingsManager.javaClass.getResourceAsStream(
            "weave/mappings/srg/${gameVersion.mappingName}"
        )
        srgStream?.use { bundledFile.writeBytes(it.readBytes()) }
            ?: { println("Could not retrieve SRG mappings for ${gameVersion.mappingName}"); failCount++ }

        val yarnStream = MappingsManager.javaClass.getResourceAsStream(
            "weave/mappings/yarn/${gameVersion.mappingName}"
        )
        yarnStream?.use { bundledFile.appendBytes(it.readBytes()) }
            ?: { println("Could not retrieve Yarn mappings for ${gameVersion.mappingName}"); failCount++ }

        downloadMojangMappings()?.also { bundledFile.appendBytes(it.readBytes()) }
            ?:{ println("Could not retrieve Mojang mappings for ${gameVersion.mappingName}"); failCount++ }

        if (failCount == 3)
            error("Failed to retrieve any mappings for ${gameVersion.mappingName}")

        return bundledFile
    }

    fun getMojangMappings(): File? {
        val file = FileManager.CACHE_DIRECTORY
            .resolve("mappings")
            .resolve(gameVersion.versionName)
            .resolve("mojang.mappings").toFile()

        if (!file.exists())
            return downloadMojangMappings()

        return file
    }

    fun getSrgMappings(): List<String>? {
        val stream = MappingsManager.javaClass.getResourceAsStream(
            "weave/mappings/srg/${gameVersion.mappingName}"
        ) ?: return null

        return stream.toLines()
    }

    fun getYarnMappings(): List<String>? {
        val stream = MappingsManager.javaClass.getResourceAsStream(
            "weave/mappings/yarn/${gameVersion.mappingName}"
        ) ?: return null

        return stream.toLines()
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
            val path = FileManager.CACHE_DIRECTORY
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