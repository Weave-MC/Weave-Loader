import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.util.*

open class CreateRelocationData : DefaultTask() {
    @get:Input
    val shadedPackage: Property<String> =
        project.objects.property(String::class.java)

    @get:Input
    val relocationList: ListProperty<String> =
        project.objects.listProperty(String::class.java)

    @get:OutputFile
    val propertiesFile: RegularFileProperty =
        project.objects.fileProperty().convention(
            project.layout.buildDirectory.file("tmp/weave-relocation-data.properties")
        )

    @TaskAction
    fun createRelocationData() {
        val properties = Properties()
        properties["target"] = shadedPackage.get()
        properties["packages"] = relocationList.get().joinToString(";")

        val outputStream = propertiesFile.get().asFile.outputStream()
        outputStream.use {
            properties.store(it, "Relocation data for Weave-Loader")
        }
    }
}
