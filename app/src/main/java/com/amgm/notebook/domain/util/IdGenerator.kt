package com.amgm.notebook.domain.util

import java.util.UUID

/** ID 文字列の採番。 */
interface IdGenerator {
    fun newId(): String
}

/** UUID によるID採番。 */
class UuidIdGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
