package com.example.tallermoto

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "trabajos")
data class Trabajo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cliente: String,
    val moto: String,
    val descripcion: String,
    val costo: Double,
    val terminado: Boolean = false,
    val fecha: Long = System.currentTimeMillis()
)

@Dao
interface TrabajoDao {
    @Query("SELECT * FROM trabajos ORDER BY terminado ASC, fecha DESC")
    fun observar(): Flow<List<Trabajo>>

    @Insert
    suspend fun insertar(trabajo: Trabajo)

    @Update
    suspend fun actualizar(trabajo: Trabajo)

    @Delete
    suspend fun borrar(trabajo: Trabajo)
}

@Database(entities = [Trabajo::class], version = 1, exportSchema = false)
abstract class TallerDb : RoomDatabase() {
    abstract fun trabajoDao(): TrabajoDao

    companion object {
        @Volatile private var instancia: TallerDb? = null

        fun get(context: Context): TallerDb =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    TallerDb::class.java,
                    "taller.db"
                ).build().also { instancia = it }
            }
    }
}
