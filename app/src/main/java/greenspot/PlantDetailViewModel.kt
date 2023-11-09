package greenspot

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.io.File
import java.util.*

class PlantDetailViewModel : ViewModel() {
    private val plantRepository: PlantRepository = PlantRepository.get()
    private val crimeIdLiveData = MutableLiveData<UUID>()

    var plantLiveData: LiveData<Plant?> = Transformations.switchMap(crimeIdLiveData) {
        plantRepository.getCrime(it)
    }

    fun loadCrime(crimeId: UUID) {
        crimeIdLiveData.value = crimeId
    }

    fun saveCrime(plant: Plant) {
        plantRepository.updateCrime(plant)
    }

    fun getPhotoFile(plant: Plant): File {
        return plantRepository.getPhotoFile(plant)
    }
}