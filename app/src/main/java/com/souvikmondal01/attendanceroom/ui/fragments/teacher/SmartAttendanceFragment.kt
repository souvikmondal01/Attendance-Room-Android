package com.souvikmondal01.attendanceroom.ui.fragments.teacher

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.souvikmondal01.attendanceroom.R
import com.souvikmondal01.attendanceroom.check_network_connectivity.NetworkViewModel
import com.souvikmondal01.attendanceroom.data.models.Attendance
import com.souvikmondal01.attendanceroom.databinding.FragmentSmartAttendanceBinding
import com.souvikmondal01.attendanceroom.location.LocationViewModel
import com.souvikmondal01.attendanceroom.ui.viewmodels.SharedViewModel
import com.souvikmondal01.attendanceroom.ui.viewmodels.teacher.SmartAttendanceViewModel
import com.souvikmondal01.attendanceroom.utils.Common.date
import com.souvikmondal01.attendanceroom.utils.Common.datePicker
import com.souvikmondal01.attendanceroom.utils.Constant.SMART_ATTENDANCE
import com.souvikmondal01.attendanceroom.utils.Response
import com.souvikmondal01.attendanceroom.utils.gone
import com.souvikmondal01.attendanceroom.utils.hideKeyboard
import com.souvikmondal01.attendanceroom.utils.snackBar
import com.souvikmondal01.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.random.Random

@AndroidEntryPoint
class SmartAttendanceFragment : Fragment() {
    private var _binding: FragmentSmartAttendanceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SmartAttendanceViewModel by viewModels()
    private val locationViewModel: LocationViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSmartAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeNetworkConnectionAndHandleUI()
        createButtonOnClick()
        whenAttendanceCardCreated()

        val randomNumber = Random.nextInt(1000, 9999).toString()
        viewModel.codeTemp.value = randomNumber
        binding.apply {
            etCode.setText(randomNumber)
            etDate.setText(date())
            etDate.setOnClickListener {
                datePicker(etDate)
            }
            vClose.setOnClickListener {
                findNavController().navigateUp()
            }
        }

        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { code ->
            lifecycleScope.launch {
                viewModel.getClassRoomDetails(code).collectLatest {
                    binding.apply {
                        etLatitude.setText(it.data?.latitude)
                        etLongitude.setText(it.data?.longitude)
                        etRadius.setText(it.data?.radius)
                    }
                }
            }
        }

        locationViewModel.getLocation(requireActivity())

        binding.cvLocation.setOnClickListener {
            lifecycleScope.launch {
                locationViewModel.getLocation.collectLatest {
                    binding.apply {
                        etLatitude.setText(it.latitude.toString())
                        etLongitude.setText(it.longitude.toString())
                    }
                }
            }
        }

        enableCreateButtonAccordingEdittextInput()
    }

    override fun onStart() {
        super.onStart()
        bottomNav = requireActivity().findViewById(R.id.bottom_navigation)
        bottomNav.gone()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun observeNetworkConnectionAndHandleUI() {
        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest {
                if (!it) {
                    snackBar("No internet connection found")
                }
            }
        }
    }

    private fun createButtonOnClick() {
        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest { isConnected ->
                binding.btnCreate.setOnClickListener {
                    if (isConnected) {
                        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { classCode ->
                            lifecycleScope.launch {
                                viewModel.isAttendanceCardListEmpty(classCode).collectLatest {
                                    if (it) {
                                        val date = binding.etDate.text.toString().trim()
                                        val code = binding.etCode.text.toString().trim()
                                        val notes = binding.etNotes.text.toString().trim()
                                        val latitude = binding.etLatitude.text.toString().trim()
                                        val longitude = binding.etLongitude.text.toString().trim()
                                        val radius = binding.etRadius.text.toString().trim()

                                        val attendance = Attendance(
                                            date = date,
                                            code = code,
                                            notes = notes,
                                            latitude = latitude,
                                            longitude = longitude,
                                            radius = radius,
                                            attendanceType = SMART_ATTENDANCE,
                                        )
                                        viewModel.createAttendanceCard(attendance, classCode)
                                    } else {
                                        snackBar("An active attendance card already exists, so you can't create a new one.")
                                    }
                                }
                            }
                        }

                    } else {
                        hideKeyboard()
                        snackBar("Something went wrong, check your internet connection and try again.")
                    }
                }
            }
        }
    }

    private fun whenAttendanceCardCreated() {
        lifecycleScope.launch {
            viewModel.isAttendanceCardCreated.collectLatest {
                when (it) {
                    is Response.Loading -> {
                        binding.progressBar.visible()
                        binding.btnCreate.isEnabled = false
                    }

                    is Response.Success -> {
                        binding.progressBar.gone()
                        binding.btnCreate.isEnabled = true
                        findNavController().navigateUp()
                    }

                    is Response.Error -> {
                        binding.progressBar.gone()
                        binding.btnCreate.isEnabled = true
                    }
                }
            }

        }
    }

    private fun enableCreateButtonAccordingEdittextInput() {
        binding.etCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {}
            override fun onTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {
                viewModel.codeTemp.value = p0.trim().toString()
                viewModel.latitudeTemp.observe(viewLifecycleOwner) { lat ->
                    viewModel.longitudeTemp.observe(viewLifecycleOwner) { long ->
                        viewModel.radiusTemp.observe(viewLifecycleOwner) { rad ->
                            viewModel.codeTemp.observe(viewLifecycleOwner) { code ->
                                binding.btnCreate.isEnabled = p0.trim()
                                    .isNotEmpty() && lat.isNotEmpty() && long.isNotEmpty() && rad.isNotEmpty() && code.length == 4
                            }
                        }
                    }
                }

            }
        })

        binding.etLatitude.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {}
            override fun onTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {
                viewModel.latitudeTemp.value = p0.trim().toString()
                viewModel.codeTemp.observe(viewLifecycleOwner) { code ->
                    viewModel.longitudeTemp.observe(viewLifecycleOwner) { long ->
                        viewModel.radiusTemp.observe(viewLifecycleOwner) { rad ->

                            binding.btnCreate.isEnabled = p0.trim()
                                .isNotEmpty() && code.isNotEmpty() && long.isNotEmpty() && rad.isNotEmpty() && code.length == 4
                        }
                    }
                }

            }
        })

        binding.etLongitude.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {}
            override fun onTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {
                viewModel.longitudeTemp.value = p0.trim().toString()
                viewModel.latitudeTemp.observe(viewLifecycleOwner) { lat ->
                    viewModel.codeTemp.observe(viewLifecycleOwner) { code ->
                        viewModel.radiusTemp.observe(viewLifecycleOwner) { rad ->

                            binding.btnCreate.isEnabled = p0.trim()
                                .isNotEmpty() && code.isNotEmpty() && lat.isNotEmpty() && rad.isNotEmpty() && code.length == 4
                        }
                    }
                }
            }
        })

        binding.etRadius.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {}
            override fun onTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {
                viewModel.radiusTemp.value = p0.trim().toString()
                viewModel.latitudeTemp.observe(viewLifecycleOwner) { lat ->
                    viewModel.longitudeTemp.observe(viewLifecycleOwner) { long ->
                        viewModel.codeTemp.observe(viewLifecycleOwner) { code ->

                            binding.btnCreate.isEnabled = p0.trim()
                                .isNotEmpty() && code.isNotEmpty() && long.isNotEmpty() && lat.isNotEmpty() && code.length == 4
                        }
                    }
                }
            }
        })
    }

}
