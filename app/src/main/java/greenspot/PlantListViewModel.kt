package greenspot

import androidx.lifecycle.ViewModel

class PlantListViewModel : ViewModel() {
    private val plantRepository = PlantRepository.get()
    val crimeListLiveData = plantRepository.getCrimes()

    fun addCrime(plant: Plant) {
        plantRepository.addCrime(plant)
    }
}