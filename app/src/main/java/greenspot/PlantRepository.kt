package greenspot

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import greenspot.database.PlantDao
import greenspot.database.PlantDatabase
import greenspot.database.migration_2_3
import java.io.File
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

private const val DATABASE_NAME = "crime-database"

class PlantRepository private constructor(context: Context) {
    private val database: PlantDatabase =
        Room.databaseBuilder(context.applicationContext, PlantDatabase::class.java, DATABASE_NAME)
            .addMigrations(migration_2_3)
            .build()
    private val plantDao: PlantDao = database.plantDao()

    private val executor: Executor = Executors.newSingleThreadExecutor()

    private val filesDir = context.applicationContext.filesDir

    fun getCrimes(): LiveData<List<Plant>> = plantDao.getCrimes()

    fun getCrime(id: UUID): LiveData<Plant?> = plantDao.getCrime(id)

    fun updateCrime(plant: Plant) {
        executor.execute {
            plantDao.updateCrime(plant)
        }
    }

    fun addCrime(plant: Plant) {
        executor.execute {
            plantDao.addCrime(plant)
        }
    }

    fun getPhotoFile(plant: Plant): File = File(filesDir, plant.photoFileName)

    companion object {
        private var INSTANCE: PlantRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = PlantRepository(context)
            }
        }

        fun get(): PlantRepository {
            return INSTANCE ?: throw IllegalStateException("CrimeRepository must be initialized")
        }
    }

}
