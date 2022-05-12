package org.ktorm.ksp.api

import org.ktorm.entity.EntitySequence

public fun EntitySequence<*, *>.checkIfSequenceModified() {
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
