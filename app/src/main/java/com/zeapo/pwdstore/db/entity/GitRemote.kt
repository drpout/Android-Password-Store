package com.zeapo.pwdstore.db.entity

import androidx.room.Embedded

data class GitRemote(
    val auth: Auth,
    @Embedded val sshKey: SshKey?
) {
   enum class Auth { SSH_KEY, PGP, USER_PASS }
}
