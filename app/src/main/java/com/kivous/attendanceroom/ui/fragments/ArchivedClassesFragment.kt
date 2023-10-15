package com.kivous.attendanceroom.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kivous.attendanceroom.R
import com.kivous.attendanceroom.check_network_connectivity.NetworkViewModel
import com.kivous.attendanceroom.data.models.ClassRoom
import com.kivous.attendanceroom.databinding.FragmentArchivedClassesBinding
import com.kivous.attendanceroom.ui.adapters.ClassRoomAdapter
import com.kivous.attendanceroom.ui.viewmodels.AppViewModel
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.snackBar
import com.kivous.attendanceroom.utils.toast
import com.kivous.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ArchivedClassesFragment : Fragment() {
    private var _binding: FragmentArchivedClassesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AppViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private lateinit var adapter: ClassRoomAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArchivedClassesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vBackArrow.setOnClickListener {
            findNavController().navigateUp()
        }
        whenNoInternet()
        setUpRecyclerView()
        ifClassRoomListEmptyShowEmpty()
        whenClassRestored()
        whenClassDeleted()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun whenClassDeleted() {
        lifecycleScope.launch {
            viewModel.isClassDeleted.collectLatest {
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

    private fun whenClassRestored() {
        lifecycleScope.launch {
            viewModel.isClassRestored.collectLatest {
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

    private fun ifClassRoomListEmptyShowEmpty() {
        lifecycleScope.launch {
            viewModel.isArchiveClassListEmpty.collectLatest {
                if (it) {
                    binding.tvNotFound.visible()
                    binding.ivNotFound.visible()
                } else {
                    binding.tvNotFound.gone()
                    binding.ivNotFound.gone()
                }
            }
        }
    }

    private fun setUpRecyclerView() {
        lifecycleScope.launch {
            viewModel.archivedClassRoomFirestoreRecyclerOptions.collectLatest {
                when (it) {
                    is Response.Loading -> {
                        binding.progressBar.visible()
                    }

                    is Response.Success -> {
                        binding.progressBar.gone()
                        try {
                            adapter = it.data?.let { it1 -> ClassRoomAdapter(it1, ::classRoomAdapterViewController) }!!
                        } catch (e: Exception) {
                            toast(e.message.toString())
                        }
                        binding.recyclerView.adapter = adapter
                        binding.recyclerView.setHasFixedSize(true)
                        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
                        adapter.startListening()
                    }

                    is Response.Error -> {
                        binding.progressBar.gone()
                    }
                }
            }
        }
    }

    private fun classRoomAdapterViewController(
        holder: ClassRoomAdapter.ViewHolder, model: ClassRoom
    ) {
        holder.apply {
            binding.apply {
                tvClassName.text = model.className
                tvDepartment.text = model.department

                vThreeDot.setOnClickListener {
                    val popUpMenu = PopupMenu(requireContext(), vThreeDot)
                    popUpMenu.menuInflater.inflate(R.menu.popup_menu_two_options, popUpMenu.menu)
                    popUpMenu.menu.findItem(R.id.one).title = "Restore"
                    popUpMenu.menu.findItem(R.id.two).title = "Delete"

                    lifecycleScope.launch {
                        networkViewModel.isConnected.collectLatest { isConnected ->
                            popUpMenu.setOnMenuItemClickListener {
                                when (it.itemId) {
                                    R.id.one -> {
                                        if (isConnected) {
                                            viewModel.restoreClassroom(model.code.toString())
                                        } else {
                                            snackBar("Connect to internet to restore the class")
                                        }

                                    }

                                    R.id.two -> {
                                        if (isConnected) {
                                            MaterialAlertDialogBuilder(requireContext()).setTitle("Delete ${model.className}?")
                                                .setPositiveButton("Delete") { _, _ ->
                                                    viewModel.deleteClassroom(model.code.toString())
                                                }.setNegativeButton("Cancel") { dialog, _ ->
                                                    dialog.dismiss()
                                                }.setCancelable(true).show()
                                        } else {
                                            snackBar("Connect to internet to delete the class")
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

}