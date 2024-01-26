package com.kivous.attendanceroom.ui.fragments.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.kivous.attendanceroom.R
import com.kivous.attendanceroom.check_network_connectivity.NetworkViewModel
import com.kivous.attendanceroom.data.models.ClassRoom
import com.kivous.attendanceroom.data.models.Location
import com.kivous.attendanceroom.databinding.FragmentHomeBinding
import com.kivous.attendanceroom.databinding.FragmentHomeBottomSheetBinding
import com.kivous.attendanceroom.ui.adapters.ClassRoomAdapter
import com.kivous.attendanceroom.ui.viewmodels.SharedViewModel
import com.kivous.attendanceroom.ui.viewmodels.common.HomeViewModel
import com.kivous.attendanceroom.utils.Common.hasLocationPermission
import com.kivous.attendanceroom.utils.Constant
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.clipBoard
import com.kivous.attendanceroom.utils.glideCircle
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.snackBar
import com.kivous.attendanceroom.utils.toast
import com.kivous.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var adapter: ClassRoomAdapter
    private lateinit var bottomSheet: HomeBottomSheetFragment

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            ivProfile.glideCircle(requireContext(), auth.currentUser?.photoUrl)

            vBackArrow.setOnClickListener {
                requireActivity().finish()
            }

            vProfile.setOnClickListener {
                findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
            }

        }

        bottomSheet = HomeBottomSheetFragment(::bottomSheetViewController)

        observeNetworkConnectionAndHandleUI()
        ifClassRoomListEmptyShowEmpty()
        fabButtonOnClick()
        setRecyclerView()
        whenClassArchived() // Teacher feature
        whenUnEnrolledFromClass() // Student feature

        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest {
                if (it) {
                    setUserLocationToFirestore()
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (!hasLocationPermission()) {
            findNavController().navigate(R.id.action_homeFragment_to_permissionFragment)
        }
    }

    override fun onPause() {
        super.onPause()
        if (bottomSheet.isVisible) {
            bottomSheet.dismissNow()
        }
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun observeNetworkConnectionAndHandleUI() {
        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest {
                binding.viewNetworkError.isVisible = !it
            }
        }
    }

    private fun setRecyclerView() {
        lifecycleScope.launch {
            viewModel.getClassRoomFirestoreRecyclerOptions().collectLatest {
                when (it) {
                    is Response.Loading -> {
                        binding.progressBar.visible()
                    }

                    is Response.Success -> {
                        binding.progressBar.gone()
                        try {
                            adapter = it.data?.let { it1 ->
                                ClassRoomAdapter(it1, ::classRoomAdapterViewController)
                            }!!
                        } catch (e: Exception) {
                            toast(e.message.toString())
                        }
                        binding.recyclerView.adapter = adapter
                        binding.recyclerView.setHasFixedSize(true)
                        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
                        adapter.startListening()
                    }

                    is Response.Error -> {
                        binding.progressBar.gone()
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun classRoomAdapterViewController(
        holder: ClassRoomAdapter.ViewHolder, model: ClassRoom
    ) {
        val userEmail = auth.currentUser!!.email.toString()
        holder.binding.apply {
            tvClassName.text = model.className
            tvDepartment.text = model.department
        }

        if (userEmail in model.teacherEmailList!!) {
            holder.apply {
                binding.apply {
                    val studentCount = model.studentEmailList?.size
                    tvCreator.text =
                        if (studentCount == 1) "$studentCount student" else "$studentCount students"

                    vThreeDot.setOnClickListener {
                        val popupMenu = PopupMenu(requireContext(), ivThreeDot)
                        popupMenu.menuInflater.inflate(
                            R.menu.popup_menu_three_options, popupMenu.menu
                        )
                        popupMenu.menu.findItem(R.id.one).title = "Edit"
                        popupMenu.menu.findItem(R.id.two).title = "Archive"
                        popupMenu.menu.findItem(R.id.three).title = "Copy join code"

                        lifecycleScope.launch {
                            networkViewModel.isConnected.collectLatest { isConnected ->
                                popupMenu.setOnMenuItemClickListener {
                                    when (it.itemId) {
                                        R.id.one -> {
                                            if (isConnected) {
                                                val bundle = Bundle()
                                                bundle.putString(
                                                    Constant.CODE, model.code.toString()
                                                )
                                                findNavController().navigate(
                                                    R.id.action_homeFragment_to_editClassFragment,
                                                    bundle
                                                )

                                            } else {
                                                snackBar("Connect to internet to edit the class")
                                            }
                                        }

                                        R.id.two -> {
                                            if (isConnected) {
                                                MaterialAlertDialogBuilder(
                                                    requireContext()
                                                ).setTitle("Archive class?").setMessage(
                                                    "You and your students won't be able to make changes.\n\nYou can view this class in Archived classes."
                                                ).setPositiveButton("Archive") { _, _ ->
                                                    viewModel.archiveClassroom(model.code.toString())
                                                }.setNegativeButton("Cancel") { dialog, _ ->
                                                    dialog.dismiss()
                                                }.setCancelable(true).show()

                                            } else {
                                                snackBar("Connect to internet to archive the class")
                                            }
                                        }

                                        R.id.three -> {
                                            clipBoard(model.code.toString())
                                        }
                                    }
                                    true
                                }
                            }

                        }

                        popupMenu.show()
                    }

                }

                itemView.setOnClickListener {
                    sharedViewModel.shareClassCode(model.code.toString())
                    findNavController().navigate(
                        R.id.action_homeFragment_to_teacherRoomFragment
                    )
                }

            }
        }

        if (userEmail in model.studentEmailList!!) {
            holder.apply {
                binding.apply {
                    tvCreator.text = model.creatorName

                    vThreeDot.setOnClickListener {
                        val popupMenu = PopupMenu(requireContext(), ivThreeDot)
                        popupMenu.menuInflater.inflate(
                            R.menu.popup_menu_one_option, popupMenu.menu
                        )
                        popupMenu.menu.findItem(R.id.one).title = "Unenroll"

                        lifecycleScope.launch {
                            networkViewModel.isConnected.collectLatest { isConnected ->
                                popupMenu.setOnMenuItemClickListener {
                                    when (it.itemId) {
                                        R.id.one -> {
                                            if (isConnected) {
                                                MaterialAlertDialogBuilder(
                                                    requireContext()
                                                ).setTitle("Unenroll from ${model.className}?")
                                                    .setMessage("You won't be able to see or participate in the class anymore.")
                                                    .setPositiveButton("Unenroll") { _, _ ->
                                                        viewModel.unEnrollFromClass(model.code.toString())
                                                    }.setNegativeButton("Cancel") { dialog, _ ->
                                                        dialog.dismiss()
                                                    }.setCancelable(true).show()
                                            } else {
                                                snackBar("Connect to internet to unenroll")
                                            }
                                        }

                                    }
                                    true
                                }
                            }
                        }

                        popupMenu.show()
                    }
                }

                itemView.setOnClickListener {
                    sharedViewModel.shareClassCode(model.code.toString())
                    findNavController().navigate(
                        R.id.action_homeFragment_to_studentRoomFragment
                    )
                }
            }
        }

    }


    private fun fabButtonOnClick() {
        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest { isConnected ->
                binding.fab.setOnClickListener {
                    if (isConnected) {
                        bottomSheet.show(childFragmentManager, HomeBottomSheetFragment.TAG)
                    } else {
                        snackBar("Connect to internet to join or create class")
                    }
                }
            }
        }
    }

    private fun bottomSheetViewController(binding: FragmentHomeBottomSheetBinding) {
        binding.vCreateClass.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_createClassFragment)
            bottomSheet.dismiss()
        }
        binding.vJoinClass.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_joinClassFragment)
            bottomSheet.dismiss()
        }
    }

    private fun whenClassArchived() {
        lifecycleScope.launch {
            viewModel.isClassArchived.collectLatest {
                when (it) {
                    is Response.Loading -> {
                        binding.progressBar.visible()
                    }

                    is Response.Success -> {
                        binding.progressBar.gone()
                    }

                    is Response.Error -> {
                        binding.progressBar.gone()
                        if (it.message.toString().isNotEmpty()) {
                            toast(it.message.toString())
                        }
                    }
                }
            }
        }
    }

    private fun whenUnEnrolledFromClass() {
        lifecycleScope.launch {
            viewModel.isUnEnrolledFromClass.collectLatest {
                when (it) {
                    is Response.Loading -> {
                        binding.progressBar.visible()
                    }

                    is Response.Success -> {
                        binding.progressBar.gone()
                    }

                    is Response.Error -> {
                        binding.progressBar.gone()
                        if (it.message.toString().isNotEmpty()) {
                            toast(it.message.toString())
                        }
                    }
                }
            }
        }
    }

    private fun ifClassRoomListEmptyShowEmpty() {
        lifecycleScope.launch {
            viewModel.isClassListEmpty().collectLatest {
                binding.apply {
                    tvNotFound.isVisible = it
                    ivNotFound.isVisible = it
                    recyclerView.isVisible = !it
                }
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        val notificationManager = NotificationManagerCompat.from(requireContext())
        return notificationManager.areNotificationsEnabled()
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun askForNotificationPermission() {
        if (!hasNotificationPermission()) {
            val permissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.POST_NOTIFICATIONS
                )
            } else {

            }
            if (permissionState == PackageManager.PERMISSION_DENIED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                        requireActivity(), arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1
                    )
                }
            }
        }
    }


    private fun setUserLocationToFirestore() {
        val fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener {
            if (it != null) {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                val area = addresses?.get(0)?.featureName
                val locality = addresses?.get(0)?.subLocality
                val city = addresses?.get(0)?.locality
                val state = addresses?.get(0)?.adminArea
                val country = addresses?.get(0)?.countryName
                val pin = addresses?.get(0)?.postalCode
                val fullAddress = addresses?.get(0)?.getAddressLine(0)
                val latitude = addresses?.get(0)?.latitude.toString()
                val longitude = addresses?.get(0)?.longitude.toString()

                val location = Location(
                    area, locality, city, state, country, pin, fullAddress, latitude, longitude
                )
                viewModel.setLocationToFirestore(location)
            }
        }

    }
}