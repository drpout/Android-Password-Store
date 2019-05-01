package com.zeapo.pwdpgpKey.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.zeapo.pwdstore.db.entity.PgpKeyEntity

@Dao
interface PgpKeyDao {
    @Insert
    fun insert(pgpKey: PgpKeyEntity)

    @Delete
    fun delete(pgpKey: PgpKeyEntity)

    @Query("SELECT * FROM pgp_key WHERE store_name LIKE :storeName")
    fun getAll(storeName: String): List<PgpKeyEntity>
}