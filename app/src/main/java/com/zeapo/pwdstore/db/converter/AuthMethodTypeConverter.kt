package com.zeapo.pwdstore.db.converter

import androidx.room.TypeConverter
import com.zeapo.pwdstore.db.entity.AuthMethod

class AuthMethodTypeConverter {
    @TypeConverter
    fun stringToAuthMethod(name: String): AuthMethod = AuthMethod.valueOf(name)

    @TypeConverter
    fun authMethodtoString(auth: AuthMethod): String = auth.name
}