package com.souvikmondal01.attendanceroom.ui.fragments.student

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.souvikmondal01.attendanceroom.R
import com.souvikmondal01.attendanceroom.check_network_connectivity.NetworkViewModel
import com.souvikmondal01.attendanceroom.data.models.Attendance
import com.souvikmondal01.attendanceroom.databinding.FragmentAttendanceStudentBinding
import com.souvikmondal01.attendanceroom.location.LocationViewModel
import com.souvikmondal01.attendanceroom.ui.adapters.AttendanceCardStudentAdapter
import com.souvikmondal01.attendanceroom.ui.viewmodels.SharedViewModel
import com.souvikmondal01.attendanceroom.ui.viewmodels.student.AttendanceStudentViewModel
import com.souvikmondal01.attendanceroom.utils.Common.androidId
import com.souvikmondal01.attendanceroom.utils.Response
import com.souvikmondal01.attendanceroom.utils.glideCircle
import com.souvikmondal01.attendanceroom.utils.gone
import com.souvikmondal01.attendanceroom.utils.hideKeyboard
import com.souvikmondal01.attendanceroom.utils.logD
import com.souvikmondal01.attendanceroom.utils.snackBar
import com.souvikmondal01.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AttendanceStudentFragment : Fragment() {
    private var _binding: FragmentAttendanceStudentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AttendanceStudentViewModel by viewModels()
    private val locationViewModel: LocationViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
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

        aboutIconOnClick()

        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { code ->
            lifecycleScope.launch {
                viewModel.getClassRoomDetails(code).collectLatest {
                    binding.tvClassName.text = it.data?.className
                    binding.tvDepartment.text = it.data?.department
                }
            }

            setRecyclerView(code)

            lifecycleScope.launch {
                viewModel.isAttendanceCardListEmpty(code).collectLatest {
                    binding.recyclerView.isVisible = !it
                }
            }
        }

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

    private fun setRecyclerView(code: String) {
        lifecycleScope.launch {
            viewModel.getAttendanceCardTeacherRecyclerOptions(code).collectLatest {
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

        holder.binding.apply {
            ivProfilePhoto.glideCircle(requireContext(), model.creatorPhotoUrl)
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
                                        com.google.android.material.R.attr.colorOnErrorContainer,
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
                                                androidId(), classCode, model.id.toString()
                                            ).let {
                                                whenAttendanceResponseGiven()
                                                viewModel.setAttendanceHistoryStudent(
                                                    classCode, model
                                                )
                                            }
                                        }

                                    } else {
                                        viewModel.giveAttendanceResponse(
                                            androidId(), classCode, model.id.toString()
                                        ).let {
                                            whenAttendanceResponseGiven()
                                            viewModel.setAttendanceHistoryStudent(
                                                classCode, model
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