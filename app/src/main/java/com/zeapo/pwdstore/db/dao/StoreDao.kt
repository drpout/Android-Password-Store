package com.zeapo.pwdstore.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query
import com.zeapo.pwdstore.db.entity.StoreEntity

@Dao
interface StoreDao {
    @Insert
    fun insert(store: StoreEntity)

    @Update
    fun update(store: StoreEntity)

    @Delete
    fun delete(store: StoreEntity)

    @Query("SELECT * FROM store")
    fun getAll(): List<StoreEntity>

    @Query("SELECT * FROM store WHERE name LIKE :name LIMIT 1")
    fun getByName(name: String): StoreEntity
}