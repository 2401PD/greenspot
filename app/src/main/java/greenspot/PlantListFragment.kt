package greenspot

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.android.greenspot.R
import java.util.*

private const val TAG = "CrimeListFragment"

class PlantListFragment : Fragment() {
    /**
     * Required interface for hosting activities
     *
     */
    interface Callbacks {
        fun onCrimeSelected(crimeId: UUID);
    }

    private var callbacks: Callbacks? = null

    private val plantListViewModel: PlantListViewModel by lazy {
        ViewModelProviders.of(this).get(PlantListViewModel::class.java)
    }

    private lateinit var crimeRecyclerView: RecyclerView;

    private var adapter: CrimeAdapter? = CrimeAdapter(emptyList())

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_plant_list, container, false)
        crimeRecyclerView = view.findViewById(R.id.crime_recycler_view) as RecyclerView
        crimeRecyclerView.layoutManager = LinearLayoutManager(context)
        crimeRecyclerView.adapter = adapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        plantListViewModel.crimeListLiveData.observe(
            viewLifecycleOwner,
            Observer { crimes ->
                crimes?.let { Log.i(TAG, "Got crimes ${crimes.size}") }
                updateUI(crimes)
            })
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_plant -> {
                val plant = Plant()
                plantListViewModel.addCrime(plant)
                callbacks?.onCrimeSelected(plant.id)
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI(plants: List<Plant>) {
        adapter = CrimeAdapter(plants)
        crimeRecyclerView.adapter = adapter
    }

    companion object {
        fun newInstance(): PlantListFragment {
            return PlantListFragment()
        }
    }

    private inner class CrimeHolder(view: View) : RecyclerView.ViewHolder(view),
        View.OnClickListener {
        private val titleTextView: TextView = itemView.findViewById(R.id.plant_name) as TextView
        private val dateTextView: TextView = itemView.findViewById(R.id.plant_date) as TextView
        private val solvedImageView: ImageView =
            itemView.findViewById(R.id.plant_found) as ImageView
        private lateinit var plant: Plant

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(plant: Plant) {
            this.plant = plant
            titleTextView.text = plant.title
            dateTextView.text = plant.date.toString()
            solvedImageView.visibility = if (plant.isSolved) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        /**
         * Called when a view has been clicked.
         *
         * @param v The view that was clicked.
         */
        override fun onClick(v: View?) {
            callbacks?.onCrimeSelected(plant.id)
        }


    }

    private inner class CrimeAdapter(var plants: List<Plant>) :
        RecyclerView.Adapter<CrimeHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeHolder {
            val view = layoutInflater.inflate(R.layout.list_item_plant, parent, false)
            return CrimeHolder(view)
        }

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            val crime = plants[position]
            holder.bind(crime)
        }

        override fun getItemCount(): Int = plants.size

    }
}