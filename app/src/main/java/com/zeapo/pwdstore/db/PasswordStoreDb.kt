import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zeapo.pwdpgpKey.db.dao.PgpKeyDao
import com.zeapo.pwdstore.db.dao.StoreDao
import com.zeapo.pwdstore.db.entity.PgpKeyEntity
import com.zeapo.pwdstore.db.entity.StoreEntity

@Database(entities = arrayOf(StoreEntity::class, PgpKeyEntity::class), version = 1)
abstract class PasswordStoreDb : RoomDatabase() {
    abstract fun storeDao(): StoreDao
    abstract fun pgpKeyDao(): PgpKeyDao

    companion object {
        private var instance: PasswordStoreDb? = null
        @Synchronized
        fun get(context: Context): PasswordStoreDb {
            if (instance == null) {
                instance = Room.databaseBuilder(context.applicationContext,
                        PasswordStoreDb::class.java, "password_store_db")
                        .build()
            }
            return instance!!
        }
    }
}
