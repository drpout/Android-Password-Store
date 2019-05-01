package com.zeapo.pwdstore.db.entity

import androidx.room.ColumnInfo

data class SshKey(
    @ColumnInfo(name = "priv_key_path") val privKeyPath: String,
    @ColumnInfo(name = "pub_key_path") val pubKeyPath: String,
    @ColumnInfo(name = "key_passphrase") val KeyPassphrase: String?
)