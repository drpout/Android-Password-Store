package com.zeapo.pwdstore.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(tableName = "pgp_key",
        foreignKeys = arrayOf(ForeignKey(
            entity = StoreEntity::class,
            parentColumns = arrayOf("name"),
            childColumns = arrayOf("store_name"))
       )
)
data class PgpKeyEntity (
    @PrimaryKey val keyId: String,
    @PrimaryKey @ColumnInfo(name = "store_name") val storeName: String
)
