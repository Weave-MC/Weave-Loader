package net.weavemc.loader.mapping

import com.grappenmaker.mappings.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.weavemc.loader.DownloadUtil
import net.weavemc.loader.JSON
import net.weavemc.loader.FileManager
import net.weavemc.api.gameVersion
import java.io.File
import java.io.InputStream
import java.net.URL
import kotlin.system.measureTimeMillis

object MappingsManager {
    private inline fun <reified T> String?.decodeJSON() = if (this != null) JSON.decodeFromString<T>(this) else null

    /**
     * Retrieves a text file containing srg, yarn, and mojang mappings
     */
    fun getOrCreateBundledMappings(): File {
        val bundledFile = FileManager.CACHE_DIRECTORY
            .resolve("mappings")
            .resolve(gameVersion.versionName)
            .resolve("bundled.mappings").toFile()

        // TODO add a way to tell what mappings are bundled in this file
        // TODO if the bundled file match the mappings weave loader is attempting to bundle, return the cached file
        // The above messages are only relevant when weave supports users to provide custom mappings
        if (bundledFile.exists()) return bundledFile
        bundledFile.parentFile.mkdirs()

        val inputs = listOf("yarn", "srg").mapNotNull {
            MappingsManager.javaClass.classLoader.getResourceAsStream(
                "weave/mappings/$it/${gameVersion.mappingName}"
            )
        } + listOfNotNull(downloadMojangMappings()?.inputStream())

        if (inputs.isEmpty()) error("Failed to retrieve any mappings for ${gameVersion.versionName}")
        println("[Weave] Bundling mappings for version ${gameVersion.mappingName}...")

        println("[Weave] Took ${measureTimeMillis {
            val joinedMappings = inputs.map {
                val baseMappings = MappingsLoader.loadMappings(it.readBytes().decodeToString().trim().lines())
                println("baseMappings = ${baseMappings.javaClass}")
                println(baseMappings.namespaces)
                when {
                    "intermediary" in baseMappings.namespaces -> baseMappings
                        .filterNamespaces("official", "named")
                        .renameNamespaces("official", "yarn")
                        .reorderNamespaces("yarn", "official")
                    baseMappings is ProguardMappings -> baseMappings.renameNamespaces("mojang", "official")
                    else -> baseMappings
                        .filterNamespaces("official", "named")
                        .reorderNamespaces("named", "official")
                        .renameNamespaces("mcp", "official")
                }
            }.join(intermediateNamespace = "official")
            
            val ordered = joinedMappings.reorderNamespaces(
                listOf("official", "yarn", "mcp", "mojang").filter { it in joinedMappings.namespaces }
            )

            bundledFile.writeText(ordered.asTinyMappings(v2 = true).write().joinToString("\n"))
        } / 1000f}s to bundle mappings")

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

    fun getSrgMappings() = MappingsManager.javaClass.classLoader.getResourceAsStream(
        "weave/mappings/srg/${gameVersion.mappingName}"
    )?.toLines()

    fun getYarnMappings() = MappingsManager.javaClass.getResourceAsStream(
        "weave/mappings/yarn/${gameVersion.mappingName}"
    )?.toLines()

    private fun InputStream.toLines(): List<String> = readBytes().decodeToString().trim().lines()

    private fun downloadMojangMappings(): File? {
        val manifest = DownloadUtil.fetch(
            "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"
        ).decodeJSON<VersionManifest>() ?: return null

        val versionEntry = manifest.versions.find { it.id == gameVersion.versionName } ?: return null
        val versionInfo = DownloadUtil.fetch(versionEntry.url).decodeJSON<VersionInfo>() ?: return null
        val mappings = versionInfo.downloads.mappings ?: return null

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
        @SerialName("client_mappings") val mappings: ClientMappings? = null // not all versions will have a client_mappings url
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