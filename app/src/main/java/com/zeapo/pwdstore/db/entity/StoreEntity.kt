package com.zeapo.pwdstore.db.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store")
data class StoreEntity (
        @PrimaryKey val name: String,
        var path: String,
        var external: Boolean,
        var initialized: Boolean,
        @ColumnInfo(name = "repo_changed") var repoChanged: Boolean,
        @Embedded var gitRemote: GitRemote?
)
