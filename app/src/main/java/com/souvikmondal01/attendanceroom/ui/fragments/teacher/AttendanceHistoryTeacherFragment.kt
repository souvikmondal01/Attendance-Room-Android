package com.souvikmondal01.attendanceroom.ui.fragments.teacher

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
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
import com.souvikmondal01.attendanceroom.databinding.FragmentAttendanceHistoryTeacherBinding
import com.souvikmondal01.attendanceroom.ui.adapters.AttendanceHistoryAdapter
import com.souvikmondal01.attendanceroom.ui.viewmodels.SharedViewModel
import com.souvikmondal01.attendanceroom.ui.viewmodels.teacher.AttendanceHistoryTeacherViewModel
import com.souvikmondal01.attendanceroom.utils.Constant.ATTENDANCE_ID
import com.souvikmondal01.attendanceroom.utils.Response
import com.souvikmondal01.attendanceroom.utils.gone
import com.souvikmondal01.attendanceroom.utils.snackBar
import com.souvikmondal01.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AttendanceHistoryTeacherFragment : Fragment() {
    private var _binding: FragmentAttendanceHistoryTeacherBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AttendanceHistoryTeacherViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var adapter: AttendanceHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceHistoryTeacherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vClose.setOnClickListener {
            findNavController().navigateUp()
        }
        observeNetworkConnectionAndHandleUI()
        setRecyclerView()
        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { code ->
            viewModel.getAttendanceHistory(code)
            whenAttendanceHistoryDeleted(code)
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

    private fun observeNetworkConnectionAndHandleUI() {
        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest {
                if (!it) {
                    snackBar("No internet connection found")
                }
            }
        }
    }

    private fun setRecyclerView() {
        lifecycleScope.launch {
            viewModel.attendanceHistoryList.collectLatest {
                when (it) {
                    is Response.Loading -> {
                        binding.progressBar.visible()
                    }

                    is Response.Success -> {
                        binding.progressBar.gone()
                        binding.recyclerView.visible()
                        adapter = AttendanceHistoryAdapter(
                            ::manualAttendanceAdapterViewController
                        )
                        binding.recyclerView.setHasFixedSize(true)
                        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
                        binding.recyclerView.adapter = adapter
                        adapter.submitList(it.data)
                        if (it.data?.isEmpty() == true) {
                            binding.recyclerView.gone()
                            binding.tvNoHistory.visible()
                        }
                    }

                    is Response.Error -> {
                        binding.progressBar.gone()
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun manualAttendanceAdapterViewController(
        holder: AttendanceHistoryAdapter.ViewHolder, attendance: Attendance
    ) {
        holder.apply {
            itemView.setOnClickListener {
                val bundle = Bundle()
                bundle.putString(ATTENDANCE_ID, attendance.id.toString())
                findNavController().navigate(
                    R.id.action_attendanceHistoryTeacherFragment_to_finalResponseListFragment,
                    bundle
                )
            }
            binding.apply {
                tvDate.text = attendance.date.toString()
                tvAttendanceType.text = attendance.attendanceType.toString()
                tvAttendanceTakenByName.text = attendance.creatorName.toString()
                tvNotes.text = attendance.notes.toString()
                tvResponseCount.text = when (attendance.finalResponseList?.size) {
                    1 -> {
                        "1  response"
                    }

                    else -> {
                        "${attendance.finalResponseList?.size}  responses"
                    }
                }

                vThreeDot.setOnClickListener {
                    val popupMenu = PopupMenu(requireContext(), vThreeDot)
                    popupMenu.menuInflater.inflate(R.menu.popup_menu_one_option, popupMenu.menu)
                    popupMenu.menu.findItem(R.id.one).title = "Delete"

                    lifecycleScope.launch {
                        networkViewModel.isConnected.collectLatest { isConnected ->
                            popupMenu.setOnMenuItemClickListener {
                                when (it.itemId) {
                                    R.id.one -> {
                                        if (isConnected) {
                                            MaterialAlertDialogBuilder(requireContext()).setTitle("Want to delete?")
                                                .setPositiveButton("Delete") { _, _ ->
                                                    sharedViewModel.sharedClassCode.observe(
                                                        viewLifecycleOwner
                                                    ) { code ->
                                                        viewModel.deleteAttendanceHistory(
                                                            code,
                                                            attendance.id.toString()
                                                        )
                                                    }

                                                }.setNegativeButton("Cancel") { dialog, _ ->
                                                    dialog.dismiss()
                                                }.setCancelable(true).show()
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
        }

    }

    private fun whenAttendanceHistoryDeleted(code: String) {
        lifecycleScope.launch {
            viewModel.isAttendanceHistoryDeleted.collectLatest {
                when (it) {
                    is Response.Loading -> {}
                    is Response.Success -> {
                        viewModel.getAttendanceHistory(code)
                    }

                    is Response.Error -> {}
                }
            }
        }
    }


}