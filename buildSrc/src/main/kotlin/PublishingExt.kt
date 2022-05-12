import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.plugins.signing.SigningExtension
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal val Project.`sourceSets`: SourceSetContainer
    get() = extensions.getByName("sourceSets") as SourceSetContainer

@OptIn(ExperimentalContracts::class)
internal fun Project.`publishing`(configure: PublishingExtension.() -> Unit): Unit {
    contract {
        callsInPlace(configure, InvocationKind.EXACTLY_ONCE)
    }
    extensions.configure("publishing", configure)
}

@OptIn(ExperimentalContracts::class)
internal fun Project.`signing`(configure: SigningExtension.() -> Unit): Unit {
    contract {
        callsInPlace(configure, InvocationKind.EXACTLY_ONCE)
    }
    extensions.configure("signing", configure)
}

val Project.publications: PublicationContainer
    get() {
        val ret: PublicationContainer
        publishing {
            ret = publications
        }
        return ret
    }
