package eu.kanade.tachiyomi.data.backup.models.metadata

import exh.metadata.sql.models.SearchTag
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

// SY --> zero-value defaults: a proto3 encoder (SyncYomi v2 server) omits zero scalars, see BackupProto3DecodeTest // SY <--
@Serializable
data class BackupSearchTag(
    @ProtoNumber(1) var namespace: String? = null,
    @ProtoNumber(2) var name: String = "",
    @ProtoNumber(3) var type: Int = 0,
) {
    fun getSearchTag(mangaId: Long): SearchTag {
        return SearchTag(
            id = null,
            mangaId = mangaId,
            namespace = namespace,
            name = name,
            type = type,
        )
    }

    companion object {
        fun copyFrom(searchTag: SearchTag): BackupSearchTag {
            return BackupSearchTag(
                namespace = searchTag.namespace,
                name = searchTag.name,
                type = searchTag.type,
            )
        }
    }
}
