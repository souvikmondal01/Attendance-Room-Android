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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kivous.attendanceroom.R
import com.kivous.attendanceroom.check_network_connectivity.NetworkViewModel
import com.kivous.attendanceroom.data.models.User
import com.kivous.attendanceroom.databinding.FragmentParticipantsStudentBinding
import com.kivous.attendanceroom.ui.adapters.StudentAdapter
import com.kivous.attendanceroom.ui.viewmodels.SharedViewModel
import com.kivous.attendanceroom.ui.viewmodels.student.ParticipantsStudentViewModel
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.glideCircle
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.logD
import com.kivous.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ParticipantsStudentFragment : Fragment() {
    private var _binding: FragmentParticipantsStudentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ParticipantsStudentViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var adapter: StudentAdapter
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentParticipantsStudentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeNetworkConnectionAndHandleUI()

        binding.vBackArrow.setOnClickListener {
            findNavController().navigateUp()
        }

        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { code ->
            setTeacherInfo(code)
            setRecyclerView(code)
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
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun observeNetworkConnectionAndHandleUI() {
        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest {
                binding.viewNetworkError.isVisible = !it
            }
        }
    }

    private fun setTeacherInfo(code: String) {
        lifecycleScope.launch {
            viewModel.getClassRoomDetails(code).collectLatest {
                binding.apply {
                    tvTitle.text = it.data?.className
                    tvTeacherName.text = it.data?.creatorName
                    ivTeacherPhoto.glideCircle(requireContext(), it.data?.creatorPhotoUrl)
                }
            }
        }
    }

    private fun setRecyclerView(code: String) {
        lifecycleScope.launch {
            viewModel.getStudentsFirestoreRecyclerOptions(code).collectLatest {
                when (it) {
                    is Response.Loading -> {
                    }

                    is Response.Success -> {
                        try {
                            adapter = it.data?.let { it1 ->
                                StudentAdapter(
                                    it1,
                                    ::studentViewController
                                )
                            }!!
                        } catch (e: Exception) {
                            logD(e.message.toString())
                        }
                        binding.recyclerViewStudents.adapter = adapter
                        binding.recyclerViewStudents.layoutManager =
                            LinearLayoutManager(requireContext())
                        adapter.startListening()
                    }

                    is Response.Error -> {
                    }
                }
            }
        }
    }

    private fun studentViewController(
        holder: StudentAdapter.ViewHolder, model: User
    ) {
        holder.binding.apply {
            ivPhoto.glideCircle(requireContext(), model.photoUrl)
            tvName.text = model.name
            ivThreeDot.gone()
        }
    }

}