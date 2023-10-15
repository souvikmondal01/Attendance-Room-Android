package com.kivous.attendanceroom.ui.fragments

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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kivous.attendanceroom.R
import com.kivous.attendanceroom.check_network_connectivity.NetworkViewModel
import com.kivous.attendanceroom.data.models.Attendance
import com.kivous.attendanceroom.databinding.FragmentAttendanceHistoryStudentBinding
import com.kivous.attendanceroom.ui.adapters.StudentAttendanceHistoryAdapter
import com.kivous.attendanceroom.ui.viewmodels.AppViewModel
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
    private val viewModel: AppViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: AppViewModel by activityViewModels()
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var adapter: StudentAttendanceHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceHistoryStudentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        whenNoInternet()
        binding.vClose.setOnClickListener {
            findNavController().navigateUp()
        }
        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) {
            viewModel.getStudentAttendanceHistoryRecyclerOptions(it)
            viewModel.getStudentAttendanceHistory(it)
        }
        setUpRecyclerView()
        whenAttendanceListEmpty()
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

    private fun whenAttendanceListEmpty() {
        lifecycleScope.launch {
            viewModel.studentAttendanceHistory.collectLatest {
                when (it) {
                    is Response.Loading -> {
                    }

                    is Response.Success -> {
                        if (it.data?.size == 0) {
                            binding.tvNoHistory.visible()
                        } else {
                            binding.tvNoHistory.gone()
                        }
                    }

                    is Response.Error -> {
                    }
                }
            }
        }
    }

    private fun setUpRecyclerView() {
        lifecycleScope.launch {
            viewModel.studentAttendanceHistoryRecyclerOptions.collectLatest {
                when (it) {
                    is Response.Loading -> {
                    }

                    is Response.Success -> {
                        try {
                            adapter = it.data?.let { it1 ->
                                StudentAttendanceHistoryAdapter(
                                    it1,
                                    ::studentAttendanceHistoryAdapterViewController
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
        holder: StudentAttendanceHistoryAdapter.ViewHolder,
        model: Attendance
    ) {
        holder.apply {
            binding.apply {
                binding.tvDate.text = model.date.toString()
                binding.tvCode.text = "Code:" + " " + model.code.toString()
                binding.tvAttendanceTakenByName.text = model.creatorName.toString()
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