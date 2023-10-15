package com.kivous.attendanceroom.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kivous.attendanceroom.R
import com.kivous.attendanceroom.check_network_connectivity.NetworkViewModel
import com.kivous.attendanceroom.databinding.FragmentAboutStudentBinding
import com.kivous.attendanceroom.ui.viewmodels.AppViewModel
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.snackBar
import com.kivous.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AboutStudentFragment : Fragment() {
    private var _binding: FragmentAboutStudentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AppViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: AppViewModel by activityViewModels()
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentAboutStudentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        whenNoInternet()
        binding.vBackArrow.setOnClickListener {
            findNavController().navigateUp()
        }
        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) {
            viewModel.getClassRoomDetails(it)
        }

        lifecycleScope.launch {
            viewModel.classRoomDetails.collectLatest {
                binding.tvClassName.text = it.data?.className

                if (it.data?.department.toString().isEmpty()) {
                    binding.tvDepartment.gone()
                    binding.tvDepartmentName.gone()
                } else {
                    binding.tvDepartment.visible()
                    binding.tvDepartmentName.visible()
                    binding.tvDepartmentName.text = it.data?.department
                }
                if (it.data?.batch.toString().isEmpty()) {
                    binding.tvBatch.gone()
                    binding.tvBatchName.gone()
                } else {
                    binding.tvBatch.visible()
                    binding.tvBatchName.visible()
                    binding.tvBatchName.text = it.data?.batch
                }
                if (it.data?.subject.toString().isEmpty()) {
                    binding.tvSubject.gone()
                    binding.tvSubjectName.gone()
                } else {
                    binding.tvSubject.visible()
                    binding.tvSubjectName.visible()
                    binding.tvSubjectName.text = it.data?.subject
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