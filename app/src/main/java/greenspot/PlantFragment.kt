package greenspot

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.bignerdranch.android.greenspot.R
import java.io.File
import java.util.*

private const val ARG_PLANT_ID = "plant_id"
private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = 0
private const val REQUEST_CONTACT = 1
private const val REQUEST_PHOTO = 2
private const val DATE_FORMAT = "EEE, MM, dd"

class PlantFragment : Fragment(), DatePickerFragment.Callbacks {
    private lateinit var plant: Plant
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView

    private val plantDetailViewModel: PlantDetailViewModel by lazy {
        ViewModelProviders.of(this).get(PlantDetailViewModel::class.java)
    }

    companion object {
        fun newInstance(crimeId: UUID): PlantFragment {
            val args = Bundle().apply {
                putSerializable(ARG_PLANT_ID, crimeId)
            }
            return PlantFragment().apply {
                arguments = args
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        plant = Plant()
        val crimeId: UUID = arguments?.getSerializable(ARG_PLANT_ID) as UUID
        plantDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_plant, container, false)
        titleField = view.findViewById(R.id.plant_name) as EditText
        dateButton = view.findViewById(R.id.plant_date) as Button
        solvedCheckBox = view.findViewById(R.id.plant_found) as CheckBox
        reportButton = view.findViewById(R.id.plant_submission) as Button
        suspectButton = view.findViewById(R.id.share_with) as Button
        photoButton = view.findViewById(R.id.crime_camera) as ImageButton
        photoView = view.findViewById(R.id.crime_photo) as ImageView

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        plantDetailViewModel.plantLiveData.observe(
            viewLifecycleOwner
        ) { crime ->
            crime?.let {
                this.plant = crime
                photoFile = plantDetailViewModel.getPhotoFile(crime)
                photoUri = FileProvider.getUriForFile(
                    requireActivity(),
                    "greenspot.fileprovider",
                    photoFile
                )
                updateUI()
            }
        }
    }

    private fun updateUI() {
        titleField.setText(plant.title)
        dateButton.text = plant.date.toString()
        solvedCheckBox.apply {
            isChecked = plant.isSolved
            jumpDrawablesToCurrentState()
        }

        if (plant.suspect.isNotEmpty()) {
            suspectButton.text = plant.suspect
        }
        updatePhotoView()
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path,requireActivity())
            photoView.setImageBitmap(bitmap)
            photoView.contentDescription = getString(R.string.plant_photo_image_description)
        } else {
            photoView.setImageBitmap(null)
            photoView.contentDescription = getString(R.string.plant_photo_no_image_description)
        }
    }

    private fun getCrimeReport(): String {
        val solvedString = if (plant.isSolved) {
            getString(R.string.plant_report_verified)
        } else {
            getString(R.string.plant_report_unverified)
        }
        val dateString = DateFormat.format(DATE_FORMAT, plant.date).toString()
        val suspect = if (plant.suspect.isBlank()) {
            getString(R.string.crime_report_no_shares)
        } else {
            getString(R.string.plant_report_shared_to, plant.suspect)
        }
        return getString(R.string.plant_submit, plant.title, dateString, solvedString, suspect)
    }

    override fun onStart() {
        super.onStart()
        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // This space intentionally left blank
            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                plant.title = sequence.toString()
            }

            override fun afterTextChanged(s: Editable?) {
                // This space intentionally left blank
            }
        }

        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked -> plant.isSolved = isChecked }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(plant.date).apply {
                setTargetFragment(this@PlantFragment, REQUEST_DATE)
                show(this@PlantFragment.requireFragmentManager(), DIALOG_DATE)
            }
        }
        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.plant_report))
            }.also { intent ->
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        suspectButton.apply {
            val pickContactIntent =
                Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CONTACT)
            }

            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(pickContactIntent, PackageManager.MATCH_DEFAULT_ONLY)

            Log.d("SuspectButton", resolvedActivity.toString())
            if (resolvedActivity == null) {
                isEnabled = false
            }
        }

        photoButton.apply {
            val packageManager: PackageManager = requireActivity().packageManager
            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(captureImage, PackageManager.MATCH_DEFAULT_ONLY)

            if (resolvedActivity === null) {
                isEnabled = false
            }

            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                val cameraActivities: List<ResolveInfo> = packageManager.queryIntentActivities(
                    captureImage,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }

                startActivityForResult(captureImage, REQUEST_PHOTO)
            }
        }

    }

    override fun onStop() {
        super.onStop()
        plantDetailViewModel.saveCrime(plant)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            resultCode != Activity.RESULT_OK -> return
            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri? = data.data
                // Specify which fields you want your query to return values for
                val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                // Perform your query - the contactUri is like a "where" clause here
                val cursor = requireActivity().contentResolver
                    .query(contactUri!!, queryFields, null, null, null)
                cursor?.use {
                    // Verify that the cursor contains at least one result
                    if (it.count == 0) {
                        return
                    }
                    // Pull out the first column of the first row of data -
                    // that is your suspect's name
                    it.moveToFirst()
                    val suspect = it.getString(0)
                    plant.suspect = suspect
                    plantDetailViewModel.saveCrime(plant)
                    suspectButton.text = suspect
                }
            }
            requestCode == REQUEST_PHOTO -> {
                requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                updatePhotoView()
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
    override fun onDateSelected(date: Date) {
        plant.date = date
        updateUI()
    }
}