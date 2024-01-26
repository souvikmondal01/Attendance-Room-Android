package com.kivous.attendanceroom.ui.fragments.student

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.kivous.attendanceroom.check_network_connectivity.NetworkViewModel
import com.kivous.attendanceroom.data.models.Attendance
import com.kivous.attendanceroom.databinding.FragmentAttendanceHistoryStudentBinding
import com.kivous.attendanceroom.ui.adapters.StudentAttendanceHistoryAdapter
import com.kivous.attendanceroom.ui.viewmodels.SharedViewModel
import com.kivous.attendanceroom.ui.viewmodels.student.AttendanceHistoryStudentViewModel
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.logD
import com.kivous.attendanceroom.utils.snackBar
import com.kivous.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AttendanceHistoryStudentFragment : Fragment() {
    private var _binding: FragmentAttendanceHistoryStudentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AttendanceHistoryStudentViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var adapter: StudentAttendanceHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceHistoryStudentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeNetworkConnectionAndHandleUI()

        binding.vClose.setOnClickListener {
            findNavController().navigateUp()
        }

        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { code ->
            setRecyclerView(code)
            whenAttendanceListEmpty(code)
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
                if (!it) {
                    snackBar("No internet connection found")
                }
            }
        }
    }

    private fun setRecyclerView(code: String) {
        lifecycleScope.launch {
            viewModel.getStudentAttendanceHistoryRecyclerOptions(code).collectLatest {
                when (it) {
                    is Response.Loading -> {
                    }

                    is Response.Success -> {
                        try {
                            adapter = it.data?.let { it1 ->
                                StudentAttendanceHistoryAdapter(
                                    it1, ::studentAttendanceHistoryAdapterViewController
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
    private fun studentAttendanceHistoryAdapterViewController(
        holder: StudentAttendanceHistoryAdapter.ViewHolder, model: Attendance
    ) {
        holder.binding.apply {
            tvDate.text = model.date.toString()
            tvCode.text = "Code:" + " " + model.code.toString()
            tvAttendanceTakenByName.text = model.creatorName.toString()
        }
    }

    private fun whenAttendanceListEmpty(code: String) {
        lifecycleScope.launch {
            viewModel.getStudentAttendanceHistory(code).collectLatest {
                when (it) {
                    is Response.Loading -> {
                    }

                    is Response.Success -> {
                        if (it.data?.size == 0) {
                            binding.tvNoHistory.visible()
                            binding.recyclerView.gone()
                        } else {
                            binding.tvNoHistory.gone()
                            binding.recyclerView.visible()
                        }
                    }

                    is Response.Error -> {
                    }
                }
            }
        }
    }

}