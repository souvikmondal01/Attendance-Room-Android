package com.kivous.attendanceroom.ui.fragments.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kivous.attendanceroom.R
import com.kivous.attendanceroom.check_network_connectivity.NetworkViewModel
import com.kivous.attendanceroom.databinding.FragmentAboutStudentBinding
import com.kivous.attendanceroom.ui.viewmodels.SharedViewModel
import com.kivous.attendanceroom.ui.viewmodels.student.AboutStudentViewModel
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.snackBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AboutStudentFragment : Fragment() {
    private var _binding: FragmentAboutStudentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AboutStudentViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
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
        observeNetworkConnectionAndHandleUI()

        binding.vBackArrow.setOnClickListener {
            findNavController().navigateUp()
        }

        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { code ->
            setClassRoomDetails(code)
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

    private fun setClassRoomDetails(code: String) {
        lifecycleScope.launch {
            viewModel.getClassRoomDetails(code).collectLatest {
                binding.apply {
                    tvClassName.text = it.data?.className
                    tvDepartmentName.text = it.data?.department
                    tvBatchName.text = it.data?.batch
                    tvSubjectName.text = it.data?.subject

                    tvDepartment.isVisible = it.data?.department.toString().isNotEmpty()
                    tvDepartmentName.isVisible = it.data?.department.toString().isNotEmpty()

                    tvBatch.isVisible = it.data?.batch.toString().isNotEmpty()
                    tvBatchName.isVisible = it.data?.batch.toString().isNotEmpty()

                    tvSubject.isVisible = it.data?.subject.toString().isNotEmpty()
                    tvSubjectName.isVisible = it.data?.subject.toString().isNotEmpty()
                }

            }
        }
    }
}