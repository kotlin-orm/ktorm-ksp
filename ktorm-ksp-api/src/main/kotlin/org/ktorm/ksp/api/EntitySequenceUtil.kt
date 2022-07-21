/*
 * Copyright 2018-2021 the original author or authors.
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

package org.ktorm.ksp.api

import org.ktorm.entity.EntitySequence

public object EntitySequenceUtil {
    /**
     * Check whether the entity sequence has been modified.
     * @throws UnsupportedOperationException If the entity sequence has been modified
     */
    public fun checkIfSequenceModified(entitySequence: EntitySequence<*, *>) {
        val expression = entitySequence.expression
        val isModified = expression.where != null
                || expression.groupBy.isNotEmpty()
                || expression.having != null
                || expression.isDistinct
                || expression.orderBy.isNotEmpty()
                || expression.offset != null
                || expression.limit != null

        if (isModified) {
            throw UnsupportedOperationException(
                "Entity manipulation functions are not supported by this sequence object. " +
                        "Please call on the origin sequence returned from database.sequenceOf(table)"
            )
        }
    }
}
