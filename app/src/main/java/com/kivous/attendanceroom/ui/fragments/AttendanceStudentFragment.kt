package com.kivous.attendanceroom.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kivous.attendanceroom.R
import com.kivous.attendanceroom.check_network_connectivity.NetworkViewModel
import com.kivous.attendanceroom.data.models.Attendance
import com.kivous.attendanceroom.databinding.FragmentAttendanceStudentBinding
import com.kivous.attendanceroom.location.LocationViewModel
import com.kivous.attendanceroom.ui.adapters.AttendanceCardStudentAdapter
import com.kivous.attendanceroom.ui.viewmodels.AppViewModel
import com.kivous.attendanceroom.utils.Common.androidId
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.glideCircle
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.hideKeyboard
import com.kivous.attendanceroom.utils.logD
import com.kivous.attendanceroom.utils.snackBar
import com.kivous.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AttendanceStudentFragment : Fragment() {
    private var _binding: FragmentAttendanceStudentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AppViewModel by viewModels()
    private val locationViewModel: LocationViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: AppViewModel by activityViewModels()
    private lateinit var adapter: AttendanceCardStudentAdapter
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceStudentBinding.inflate(inflater, container, false)
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vBackArrow.setOnClickListener {
            requireActivity().onBackPressed()
        }
        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { code ->
            viewModel.getClassRoomDetails(code)
            viewModel.getAttendanceCardTeacherRecyclerOptions(code)
        }

        lifecycleScope.launch {
            viewModel.classRoomDetails.collectLatest {
                binding.tvClassName.text = it.data?.className
                binding.tvDepartment.text = it.data?.department
            }
        }

        setUpRecyclerView()
        aboutIconOnClick()

    }


    override fun onStart() {
        super.onStart()
        bottomNav = requireActivity().findViewById(R.id.bottom_navigation)
        bottomNav.visible()
    }

    override fun onStop() {
        super.onStop()
        bottomNav.gone()
        adapter.stopListening()
        locationViewModel.stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun aboutIconOnClick() {
        binding.ivAbout.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), binding.ivAbout)
            popupMenu.menuInflater.inflate(
                R.menu.popup_menu_two_options, popupMenu.menu
            )
            popupMenu.menu.findItem(R.id.one).title = "About"
            popupMenu.menu.findItem(R.id.two).title = "Attendance history"

            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.one -> {
                        findNavController().navigate(R.id.action_attendanceStudentFragment_to_aboutStudentFragment)
                    }

                    R.id.two -> {
                        findNavController().navigate(R.id.action_attendanceStudentFragment_to_attendanceHistoryStudentFragment)
                    }
                }
                true
            }
            popupMenu.show()
        }
    }

    private fun setUpRecyclerView() {
        lifecycleScope.launch {
            viewModel.attendanceCardTeacherRecyclerOptions.collectLatest {
                when (it) {
                    is Response.Loading -> {
                    }

                    is Response.Success -> {
                        try {
                            adapter = it.data?.let { it1 ->
                                AttendanceCardStudentAdapter(
                                    it1, ::attendanceCardStudentAdapterViewController
                                )
                            }!!
                        } catch (e: Exception) {
                            logD(e.message.toString())
                        }
                        binding.recyclerView.adapter = adapter
                        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
                        adapter.startListening()
                    }

                    is Response.Error -> {
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun attendanceCardStudentAdapterViewController(
        holder: AttendanceCardStudentAdapter.ViewHolder, model: Attendance
    ) {
        locationViewModel.isInClassRoom(
            requireContext(),
            model.latitude.toString().toDouble(),
            model.longitude.toString().toDouble(),
            model.radius.toString().toDouble(),
        )

        holder.apply {
            binding.apply {
                glideCircle(model.creatorPhotoUrl, ivProfilePhoto)
                tvTeacherName.text = model.creatorName.toString()
                tvDate.text = model.date.toString()

                if (model.locationCheck == true) {
                    locationViewModel.startLocationUpdates(requireContext())
                    lifecycleScope.launch {
                        locationViewModel.isInClassRoom.collectLatest {
                            when (it) {
                                is Response.Loading -> {
                                    btnGiveResponse.isEnabled = false
                                    tvLocationCheck.text = "Location checking..."
                                    val typedValue = TypedValue()
                                    val theme = activity?.theme!!
                                    theme.resolveAttribute(
                                        com.google.android.material.R.attr.colorOnSurface,
                                        typedValue,
                                        true
                                    )
                                    val colorOnSurface = typedValue.data
                                    tvLocationCheck.setTextColor(colorOnSurface)
                                }

                                is Response.Success -> {
                                    locationViewModel.stopLocationUpdates()

                                    if (it.data == true) {
                                        btnGiveResponse.isEnabled = true
                                        tvLocationCheck.text = ""
                                    } else {
                                        btnGiveResponse.isEnabled = false
                                        tvLocationCheck.text = "You are not in the classroom."
                                        val typedValue = TypedValue()
                                        val theme = activity?.theme!!
                                        theme.resolveAttribute(
                                            com.google.android.material.R.attr.colorError,
                                            typedValue,
                                            true
                                        )
                                        val colorError = typedValue.data
                                        tvLocationCheck.setTextColor(colorError)
                                    }
                                }

                                is Response.Error -> {
                                    locationViewModel.stopLocationUpdates()
                                }
                            }
                        }
                    }
                } else {
                    btnGiveResponse.isEnabled = true
                    tvLocationCheck.text = ""
                }

                lifecycleScope.launch {
                    networkViewModel.isConnected.collectLatest { isConnected ->
                        btnGiveResponse.setOnClickListener {
                            hideKeyboard()
                            if (isConnected) {
                                if (etCode.text?.trim().toString() == model.code) {
                                    sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { classCode ->
                                        if (model.singleDeviceSingleResponse == true) {
                                            if (model.androidIdList?.contains(androidId()) == true) {
                                                MaterialAlertDialogBuilder(
                                                    requireContext()
                                                ).setMessage("A response has already been recorded for this device.")
                                                    .setNegativeButton("Dismiss") { dialog, _ ->
                                                        dialog.dismiss()
                                                    }.setCancelable(true).show()

                                            } else {
                                                viewModel.giveAttendanceResponse(
                                                    androidId(),
                                                    classCode,
                                                    model.id.toString()
                                                ).let {
                                                    whenAttendanceResponseGiven()
                                                    viewModel.setAttendanceHistoryStudent(
                                                        classCode,
                                                        model
                                                    )
                                                }
                                            }

                                        } else {
                                            viewModel.giveAttendanceResponse(
                                                androidId(),
                                                classCode,
                                                model.id.toString()
                                            ).let {
                                                whenAttendanceResponseGiven()
                                                viewModel.setAttendanceHistoryStudent(
                                                    classCode,
                                                    model
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    snackBar("Wrong code")
                                }
                            } else {
                                snackBar("Something went wrong, check your internet connection and try again.")
                            }
                        }

                    }
                }

            }
        }
    }

    private fun whenAttendanceResponseGiven() {
        lifecycleScope.launch {
            viewModel.isAttendanceResponseGiven.collectLatest {
                when (it) {
                    is Response.Loading -> {
                        this@AttendanceStudentFragment.binding.progressBar.visible()
                    }

                    is Response.Success -> {
                        snackBar("Your response has been recorded.")
                        this@AttendanceStudentFragment.binding.progressBar.gone()
                    }

                    is Response.Error -> {
                        this@AttendanceStudentFragment.binding.progressBar.gone()
                    }
                }
            }
        }
    }

}