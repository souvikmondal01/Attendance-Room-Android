package com.kivous.attendanceroom.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
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
import com.kivous.attendanceroom.data.models.User
import com.kivous.attendanceroom.databinding.FragmentParticipantsTeacherBinding
import com.kivous.attendanceroom.ui.adapters.StudentAdapter
import com.kivous.attendanceroom.ui.viewmodels.AppViewModel
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.glideCircle
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.logD
import com.kivous.attendanceroom.utils.snackBar
import com.kivous.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ParticipantsTeacherFragment : Fragment() {
    private var _binding: FragmentParticipantsTeacherBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AppViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: AppViewModel by activityViewModels()
    private lateinit var adapter: StudentAdapter
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParticipantsTeacherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        whenNoInternet()
        whenStudentRemovedByTeacher()

        binding.vBackArrow.setOnClickListener {
            findNavController().navigateUp()
        }

        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { code ->
            viewModel.getClassRoomDetails(code)
            viewModel.getStudentsFirestoreRecyclerOptions(code)
            viewModel.isStudentListEmpty(code)
        }
        showTeacherInfo()
        setUpRecyclerView()
        whenStudentListEmpty()

    }

    override fun onStart() {
        super.onStart()
        bottomNav = requireActivity().findViewById(R.id.bottom_navigation)
        bottomNav.visible()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
        bottomNav.gone()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun whenStudentRemovedByTeacher() {
        lifecycleScope.launch {
            viewModel.isStudentRemovedFromClassRoomByTeacher.collectLatest {
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

    private fun whenStudentListEmpty() {
        lifecycleScope.launch {
            viewModel.isStudentListEmpty.collectLatest {
                if (it) {
                    binding.tvNoStudents.visible()
                    binding.ivNotFound.visible()
                } else {
                    binding.tvNoStudents.gone()
                    binding.ivNotFound.gone()
                }
            }
        }
    }

    private fun setUpRecyclerView() {
        lifecycleScope.launch {
            viewModel.studentsFirestoreRecyclerOptions.collectLatest {
                when (it) {
                    is Response.Loading -> {
                    }

                    is Response.Success -> {
                        try {
                            adapter = it.data?.let { it1 -> StudentAdapter(it1, ::studentAdapterViewController) }!!
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

    private fun showTeacherInfo() {
        lifecycleScope.launch {
            viewModel.classRoomDetails.collectLatest {
                binding.tvTitle.text = it.data?.className
                glideCircle(it.data?.creatorPhotoUrl, binding.ivTeacherPhoto)
                binding.tvTeacherName.text = it.data?.creatorName
            }
        }
    }

    private fun studentAdapterViewController(
        holder: StudentAdapter.ViewHolder, model: User
    ) {
        holder.apply {
            binding.apply {
                glideCircle(model.photoUrl, ivPhoto)
                tvName.text = model.name

                vThreeDot.setOnClickListener {
                    val popUpMenu = PopupMenu(requireContext(), ivThreeDot)
                    popUpMenu.menuInflater.inflate(
                        R.menu.popup_menu_one_option, popUpMenu.menu
                    )
                    popUpMenu.menu.findItem(R.id.one).title = "Remove"
                    lifecycleScope.launch {
                        networkViewModel.isConnected.collectLatest { isConnected ->
                            popUpMenu.setOnMenuItemClickListener {
                                when (it.itemId) {
                                    R.id.one -> {
                                        if (isConnected) {
                                            sharedViewModel.sharedClassCode.observe(
                                                viewLifecycleOwner
                                            ) { code ->
                                                MaterialAlertDialogBuilder(
                                                    requireContext()
                                                ).setTitle("Remove ${model.name}?")
                                                    .setPositiveButton("Remove") { _, _ ->
                                                        viewModel.removeStudentFromClassRoomByTeacher(
                                                            code, model
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
                                true
                            }
                        }
                    }
                    popUpMenu.show()
                }
            }
        }
    }

    private fun whenNoInternet() {
        /**
         * When no internet connection show no internet warning
         */
        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest {
                if (it) {
                    binding.viewNetworkError.gone()
                } else {
                    binding.viewNetworkError.visible()
                }
            }
        }
    }


}