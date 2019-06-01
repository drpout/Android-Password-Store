package com.zeapo.pwdstore.db.entity

import androidx.room.ColumnInfo

data class SshKey(
    @ColumnInfo(name = "priv_key_path") var privKeyPath: String,
    @ColumnInfo(name = "pub_key_path") var pubKeyPath: String,
    var generated: Boolean,
    @ColumnInfo(name = "key_passphrase") var keyPassphrase: String?
)
