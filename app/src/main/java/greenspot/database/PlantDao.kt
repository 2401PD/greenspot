package greenspot.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import greenspot.Plant
import java.util.*

@Dao
interface PlantDao {
    @Query("SELECT * FROM plant")
    fun getCrimes(): LiveData<List<Plant>>

    @Query("SELECT * FROM plant WHERE id=:id")
    fun getCrime(id: UUID): LiveData<Plant?>

    @Update
    fun updateCrime(plant: Plant)

    @Insert
    fun addCrime(plant: Plant)
}
