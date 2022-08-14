/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.api.Project
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.plugins.signing.SigningExtension
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal val Project.sourceSets: SourceSetContainer
    get() = extensions.getByName("sourceSets") as SourceSetContainer

@OptIn(ExperimentalContracts::class)
internal fun Project.publishing(configure: PublishingExtension.() -> Unit): Unit {
    contract {
        callsInPlace(configure, InvocationKind.EXACTLY_ONCE)
    }
    extensions.configure("publishing", configure)
}

@OptIn(ExperimentalContracts::class)
internal fun Project.signing(configure: SigningExtension.() -> Unit): Unit {
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
