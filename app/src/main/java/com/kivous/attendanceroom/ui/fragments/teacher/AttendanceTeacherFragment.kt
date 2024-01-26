package com.kivous.attendanceroom.ui.fragments.teacher

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.kivous.attendanceroom.R
import com.kivous.attendanceroom.check_network_connectivity.NetworkViewModel
import com.kivous.attendanceroom.data.models.Attendance
import com.kivous.attendanceroom.databinding.FragmentAttendanceBottomSheetBinding
import com.kivous.attendanceroom.databinding.FragmentAttendanceTeacherBinding
import com.kivous.attendanceroom.ui.adapters.AttendanceCardTeacherAdapter
import com.kivous.attendanceroom.ui.viewmodels.SharedViewModel
import com.kivous.attendanceroom.ui.viewmodels.teacher.AttendanceTeacherViewModel
import com.kivous.attendanceroom.utils.Constant
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.logD
import com.kivous.attendanceroom.utils.snackBar
import com.kivous.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AttendanceTeacherFragment : Fragment() {
    private var _binding: FragmentAttendanceTeacherBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AttendanceTeacherViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var bottomSheet: AttendanceBottomSheetFragment
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var adapter: AttendanceCardTeacherAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceTeacherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bottomSheet = AttendanceBottomSheetFragment(::bottomSheetViewController)

        binding.apply {
            cvBackArrow.setOnClickListener {
                requireActivity().onBackPressed()
            }
            cvSetting.setOnClickListener {
                findNavController().navigate(R.id.action_attendanceTeacherFragment_to_classRoomSettingFragment)
            }
        }

        fabButtonOnClick()

        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { code ->
            setRecyclerView(code)

            lifecycleScope.launch {
                viewModel.isAttendanceCardListEmpty(code).collectLatest {
                    binding.recyclerView.isVisible = !it
                }
            }

            lifecycleScope.launch {
                viewModel.getClassRoomDetails(code).collectLatest {
                    binding.apply {
                        tvClassName.text = it.data?.className
                        tvDepartment.text = it.data?.department
                    }
                }
            }

        }

    }

    override fun onStart() {
        super.onStart()
        bottomNav = requireActivity().findViewById(R.id.bottom_navigation)
        bottomNav.visible()
    }

    override fun onPause() {
        super.onPause()
        if (bottomSheet.isVisible) {
            bottomSheet.dismissNow()
        }
    }

    override fun onStop() {
        super.onStop()
        bottomNav.gone()
        adapter.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun bottomSheetViewController(binding: FragmentAttendanceBottomSheetBinding) {
        binding.vSmartAttendance.setOnClickListener {
            findNavController().navigate(R.id.action_attendanceTeacherFragment_to_smartAttendanceFragment)
            bottomSheet.dismiss()
        }
        binding.vManualAttendance.setOnClickListener {
            findNavController().navigate(R.id.action_attendanceTeacherFragment_to_manualAttendanceFragment)
            bottomSheet.dismiss()
        }
        binding.vAttendanceHistory.setOnClickListener {
            findNavController().navigate(R.id.action_attendanceTeacherFragment_to_attendanceHistoryFragment)
            bottomSheet.dismiss()
        }
    }

    private fun fabButtonOnClick() {
        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest { isConnected ->
                binding.fab.setOnClickListener {
                    if (isConnected) {
                        bottomSheet.show(childFragmentManager, AttendanceBottomSheetFragment.TAG)
                    } else {
                        snackBar("Something went wrong, check your internet connection and try again.")
                    }
                }
            }
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
                                AttendanceCardTeacherAdapter(
                                    it1, ::attendanceCardTeacherAdapterViewController
                                )
                            }!!
                        } catch (e: Exception) {
                            logD(e.message.toString())
                        }
                        binding.recyclerView.adapter = adapter
                        binding.recyclerView.setHasFixedSize(true)
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
    private fun attendanceCardTeacherAdapterViewController(
        holder: AttendanceCardTeacherAdapter.ViewHolder, model: Attendance
    ) {
        holder.apply {
            binding.apply {
                sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { code ->
                    lifecycleScope.launch {
                        viewModel.getResponseCount(code, model.id.toString()).collectLatest {
                            when (it) {
                                0 -> {
                                    tvResponseCount.text = "Wait for responses..."
                                }

                                1 -> {
                                    tvResponseCount.text = "$it  response"
                                }

                                else -> {
                                    tvResponseCount.text = "$it  responses"
                                }
                            }
                        }
                    }
                }

                tvDate.text = model.date.toString()
                tvCode.text = "Code: ${model.code.toString()}"
                tvNotes.text = model.notes.toString()

                lifecycleScope.launch {
                    networkViewModel.isConnected.collectLatest { isConnected ->
                        btnDone.setOnClickListener {
                            if (isConnected) {
                                sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { code ->
                                    MaterialAlertDialogBuilder(
                                        requireContext()
                                    ).setTitle("Done taking attendance?")
                                        .setMessage("You can access all attendance details later in the 'Attendance History' section.")
                                        .setPositiveButton("Done") { _, _ ->
                                            viewModel.doneTakingAttendance(
                                                code, model.id.toString()
                                            )
                                            viewModel.setFinalResponseList(
                                                code, model.id.toString()
                                            )
                                        }.setNegativeButton("Cancel") { dialog, _ ->
                                            dialog.dismiss()
                                        }.setCancelable(true).show()
                                }
                            } else {
                                snackBar("Something went wrong, check your internet connection and try again.")
                            }
                        }
                    }
                }

                vThreeDot.setOnClickListener {
                    val popupMenu = PopupMenu(requireContext(), ivThreeDot)
                    popupMenu.menuInflater.inflate(
                        R.menu.popup_menu_three_options, popupMenu.menu
                    )
                    popupMenu.menu.findItem(R.id.one).title = "Delete"
                    val changeLocationCheck = popupMenu.menu.findItem(R.id.two)
                    val singleDeviceSingleResponse = popupMenu.menu.findItem(R.id.three)

                    sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { code ->
                        lifecycleScope.launch {
                            viewModel.getLocationCheckStatus(code, model.id.toString())
                                .collectLatest {
                                    if (it) {
                                        changeLocationCheck.title = "Disable location check"
                                    } else {
                                        changeLocationCheck.title = "Enable location check"
                                    }
                                }
                        }

                        lifecycleScope.launch {
                            viewModel.getSingleDeviceSingleResponseStatus(code, model.id.toString())
                                .collectLatest {
                                    if (it) {
                                        singleDeviceSingleResponse.title =
                                            "Disable single device single response"
                                    } else {
                                        singleDeviceSingleResponse.title =
                                            "Enable single device single response"
                                    }
                                }
                        }
                    }

                    lifecycleScope.launch {
                        networkViewModel.isConnected.collectLatest { isConnected ->
                            popupMenu.setOnMenuItemClickListener {
                                when (it.itemId) {
                                    R.id.one -> {
                                        if (isConnected) {
                                            sharedViewModel.sharedClassCode.observe(
                                                viewLifecycleOwner
                                            ) { code ->
                                                MaterialAlertDialogBuilder(
                                                    requireContext()
                                                ).setTitle("Want to delete?")
                                                    .setPositiveButton("Delete") { _, _ ->
                                                        viewModel.deleteAttendanceCard(
                                                            code, model.id.toString()
                                                        )
                                                    }.setNegativeButton("Cancel") { dialog, _ ->
                                                        dialog.dismiss()
                                                    }.setCancelable(true).show()
                                            }

                                        } else {
                                            snackBar("Something went wrong, check your internet connection and try again.")
                                        }
                                    }

                                    R.id.two -> {
                                        if (isConnected) {
                                            sharedViewModel.sharedClassCode.observe(
                                                viewLifecycleOwner
                                            ) { code ->
                                                if (it.title.toString() == "Disable location check") {
                                                    MaterialAlertDialogBuilder(
                                                        requireContext()
                                                    ).setTitle("Disable location check?")
                                                        .setMessage("Students can give responses from anywhere.")
                                                        .setPositiveButton("Okay") { _, _ ->
                                                            viewModel.toggleLocationCheck(
                                                                code, model.id.toString()
                                                            )
                                                        }.setNegativeButton("Cancel") { dialog, _ ->
                                                            dialog.dismiss()
                                                        }.setCancelable(true).show()

                                                } else {
                                                    viewModel.toggleLocationCheck(
                                                        code, model.id.toString()
                                                    )
                                                }
                                            }
                                        } else {
                                            snackBar("Something went wrong, check your internet connection and try again.")
                                        }
                                    }

                                    R.id.three -> {
                                        if (isConnected) {
                                            sharedViewModel.sharedClassCode.observe(
                                                viewLifecycleOwner
                                            ) { code ->
                                                if (it.title.toString() == "Disable single device single response") {
                                                    MaterialAlertDialogBuilder(
                                                        requireContext()
                                                    ).setTitle("Want to disable?")
                                                        .setMessage("Students can give responses for multiple accounts using a single device.")
                                                        .setPositiveButton("Okay") { _, _ ->
                                                            viewModel.toggleSingleDeviceSingleResponse(
                                                                code, model.id.toString()
                                                            )
                                                        }.setNegativeButton("Cancel") { dialog, _ ->
                                                            dialog.dismiss()
                                                        }.setCancelable(true).show()

                                                } else {
                                                    viewModel.toggleSingleDeviceSingleResponse(
                                                        code, model.id.toString()
                                                    )
                                                }
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

            itemView.setOnClickListener {
                val bundle = Bundle()
                bundle.putString(Constant.ATTENDANCE_ID, model.id.toString())
                findNavController().navigate(
                    R.id.action_attendanceTeacherFragment_to_responseListFragment, bundle
                )
            }

        }
    }


}