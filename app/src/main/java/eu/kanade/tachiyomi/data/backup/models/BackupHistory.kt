package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import tachiyomi.domain.history.model.History
import java.util.Date

// SY --> zero-value defaults: a proto3 encoder (SyncYomi v2 server) omits zero scalars, see BackupProto3DecodeTest // SY <--
@Serializable
data class BackupHistory(
    @ProtoNumber(1) var url: String = "",
    @ProtoNumber(2) var lastRead: Long = 0,
    @ProtoNumber(3) var readDuration: Long = 0,
) {
    fun getHistoryImpl(): History {
        return History.create().copy(
            readAt = Date(lastRead),
            readDuration = readDuration,
        )
    }
}
