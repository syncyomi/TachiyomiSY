package tachiyomi.domain.category.model

import java.io.Serializable

data class Category(
    val id: Long,
    val name: String,
    val order: Long,
    val flags: Long,
    val version: Long = 0,
    val uid: Long = 0,
    val lastModifiedAt: Long = 0,
) : Serializable {

    val isSystemCategory: Boolean = id == UNCATEGORIZED_ID

    companion object {
        const val UNCATEGORIZED_ID = 0L

        fun nameToUid(name: String): Long {
            val normalized = name.trim().lowercase()
            var h = 1125899906842597L
            for (c in normalized) {
                h = 31L * h + c.code
            }
            val positive = h and Long.MAX_VALUE
            return if (positive == 0L) 1L else positive
        }
    }
}
