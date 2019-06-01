package com.zeapo.pwdstore.db.entity

import androidx.room.Embedded
import androidx.room.TypeConverters
import com.zeapo.pwdstore.db.converter.AuthMethodTypeConverter

@TypeConverters(AuthMethodTypeConverter::class)
data class GitRemote(
    var auth: AuthMethod,
    @Embedded var sshKey: SshKey?
)
