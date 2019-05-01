package com.zeapo.pwdstore.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store")
data class StoreEntity (
    @PrimaryKey val name: String,
    val path: String,
    val external: Boolean,
    val initialized: Boolean,
    @Embedded val gitRemote: GitRemote?
)