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

package org.ktorm.ksp.compiler.generator.util

import java.util.*

public class NameAllocator {
    private val allocatedNames: MutableSet<String> = mutableSetOf()
    private val tagToName: MutableMap<Any, String> = mutableMapOf()

    public fun newName(suggestion: String, tag: Any = UUID.randomUUID().toString()): String {
        var result = suggestion
        while (!allocatedNames.add(result)) {
            result += "_"
        }
        val replaced = tagToName.put(tag, result)
        if (replaced != null) {
            tagToName[tag] = replaced // Put things back as they were!
            throw IllegalArgumentException("tag $tag cannot be used for both '$replaced' and '$result'")
        }
        return result
    }

    public operator fun get(tag: Any): String = requireNotNull(tagToName[tag]) { "unknown tag: $tag" }

}
