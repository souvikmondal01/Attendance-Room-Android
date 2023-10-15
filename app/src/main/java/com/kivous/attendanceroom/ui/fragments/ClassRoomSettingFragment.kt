package com.kivous.attendanceroom.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kivous.attendanceroom.R
import com.kivous.attendanceroom.check_network_connectivity.NetworkViewModel
import com.kivous.attendanceroom.databinding.FragmentClassRoomSettingBinding
import com.kivous.attendanceroom.location.LocationViewModel
import com.kivous.attendanceroom.ui.viewmodels.AppViewModel
import com.kivous.attendanceroom.utils.Constant
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.clipBoard
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.snackBar
import com.kivous.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ClassRoomSettingFragment : Fragment() {
    private var _binding: FragmentClassRoomSettingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AppViewModel by viewModels()
    private val locationViewModel: LocationViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: AppViewModel by activityViewModels()
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClassRoomSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        whenNoInternet()
        binding.vClose.setOnClickListener {
            findNavController().navigateUp()
        }
        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { code ->
            saveButtonOnClick(code)
            onThreeDotClickToggleClassJoinOption(code)
            binding.tvClassCode.text = "Code: $code"
            binding.cvCopy.setOnClickListener {
                clipBoard(code)
            }
            viewModel.getClassRoomDetails(code)
        }
        setClassRoomDetailsToEdittext()
        whenClassRoomDetailsUpdated()
        whenClassNameNotEmptyEnableSaveButton()

        locationViewModel.getLocation(requireActivity())
        binding.cvLocation.setOnClickListener {
            lifecycleScope.launch {
                locationViewModel.getLocation.collectLatest {
                    binding.etLatitude.setText(it.latitude.toString())
                    binding.etLongitude.setText(it.longitude.toString())
                }
            }
        }

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

    private fun whenClassNameNotEmptyEnableSaveButton() {
        binding.etClassName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.btnSave.isEnabled = p0?.trim().toString().isNotEmpty()
            }
        })
    }

    private fun whenClassRoomDetailsUpdated() {
        lifecycleScope.launch {
            viewModel.isClassRoomEdited.collectLatest {
                when (it) {
                    is Response.Loading -> {
                        binding.progressBar.visible()
                    }

                    is Response.Success -> {
                        binding.progressBar.gone()
                    }

                    is Response.Error -> {
                        binding.progressBar.gone()
                    }
                }
            }
        }
    }

    private fun setClassRoomDetailsToEdittext() {
        lifecycleScope.launch {
            viewModel.classRoomDetails.collectLatest {
                binding.etClassName.setText(it.data?.className)
                binding.etDepartment.setText(it.data?.department)
                binding.etBatch.setText(it.data?.batch)
                binding.etSubject.setText(it.data?.subject)
                binding.etLatitude.setText(it.data?.latitude)
                binding.etLongitude.setText(it.data?.longitude)
                binding.etRadius.setText(it.data?.radius)
            }
        }
    }

    private fun onThreeDotClickToggleClassJoinOption(code: String) {
        binding.cvThreeDot.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), binding.cvThreeDot)
            popupMenu.menuInflater.inflate(
                R.menu.popup_menu_one_option, popupMenu.menu
            )
            val menuItem = popupMenu.menu.findItem(R.id.one)
            lifecycleScope.launch {
                viewModel.classRoomDetails.collectLatest {
                    if (it.data?.canJoin == true) {
                        menuItem.title = "Disable"
                    } else {
                        menuItem.title = "Enable"
                    }
                }
            }
            lifecycleScope.launch {
                networkViewModel.isConnected.collectLatest { isConnected ->
                    popupMenu.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.one -> {
                                if (isConnected) {
                                    if (it.title == "Disable") {
                                        val map = mapOf(
                                            Constant.CAN_JOIN to false
                                        )
                                        MaterialAlertDialogBuilder(
                                            requireContext()
                                        ).setTitle("Want to disable?")
                                            .setMessage("Students can't join the class with the code.")
                                            .setPositiveButton("Okay") { _, _ ->
                                                viewModel.editClassroomDetails(map, code)

                                            }.setNegativeButton("Cancel") { dialog, _ ->
                                                dialog.dismiss()
                                            }.setCancelable(true).show()
                                    } else {
                                        val map = mapOf(
                                            Constant.CAN_JOIN to true
                                        )
                                        viewModel.editClassroomDetails(map, code)
                                    }

                                } else {
                                    snackBar("Something went wrong, check your internet connection and try again.")
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

    private fun saveButtonOnClick(code: String) {

        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest { isConnected ->
                binding.btnSave.setOnClickListener {
                    if (isConnected) {
                        val className = binding.etClassName.text.toString().trim()
                        val department = binding.etDepartment.text.toString().trim()
                        val batch = binding.etBatch.text.toString().trim()
                        val subject = binding.etSubject.text.toString().trim()
                        val latitude = binding.etLatitude.text.toString().trim()
                        val longitude = binding.etLongitude.text.toString().trim()
                        val radius = binding.etRadius.text.toString().trim()

                        val map = mapOf(
                            Constant.CLASS_NAME to className,
                            Constant.DEPARTMENT to department,
                            Constant.BATCH to batch,
                            Constant.SUBJECT to subject,
                            Constant.LATITUDE to latitude,
                            Constant.LONGITUDE to longitude,
                            Constant.RADIUS to radius,
                        )

                        viewModel.editClassroomDetails(map, code)

                    } else {
                        snackBar("Something went wrong, check your internet connection and try again.")
                    }
                }

            }
        }

    }

    private fun whenNoInternet() {
        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest {
                if (!it) {
                    snackBar("No internet connection found")
                }
            }
        }
    }


}