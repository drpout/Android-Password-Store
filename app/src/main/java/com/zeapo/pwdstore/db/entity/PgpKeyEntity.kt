package com.zeapo.pwdstore.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(tableName = "pgp_key",
        foreignKeys = arrayOf(ForeignKey(
            entity = StoreEntity::class,
            parentColumns = arrayOf("name"),
            childColumns = arrayOf("store_name"))
       ),
        primaryKeys = ["key_id", "store_name"]
)
data class PgpKeyEntity (
    @ColumnInfo(name = "key_id") val keyId: String,
    @ColumnInfo(name = "store_name") val storeName: String
)
