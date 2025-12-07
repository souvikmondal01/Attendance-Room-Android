package com.souvikmondal01.attendanceroom.ui.fragments.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.souvikmondal01.attendanceroom.R
import com.souvikmondal01.attendanceroom.check_network_connectivity.NetworkViewModel
import com.souvikmondal01.attendanceroom.data.models.ClassRoom
import com.souvikmondal01.attendanceroom.databinding.FragmentArchivedClassesBinding
import com.souvikmondal01.attendanceroom.ui.adapters.ClassRoomAdapter
import com.souvikmondal01.attendanceroom.ui.viewmodels.common.ArchivedClassesViewModel
import com.souvikmondal01.attendanceroom.utils.Response
import com.souvikmondal01.attendanceroom.utils.gone
import com.souvikmondal01.attendanceroom.utils.snackBar
import com.souvikmondal01.attendanceroom.utils.toast
import com.souvikmondal01.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ArchivedClassesFragment : Fragment() {
    private var _binding: FragmentArchivedClassesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ArchivedClassesViewModel by viewModels()
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

        observeNetworkConnectionAndHandleUI()
        setRecyclerView()
        whenClassRestored()
        whenClassDeleted()
        ifClassRoomListEmptyShowEmpty()
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
                binding.viewNetworkError.isVisible = !it
            }
        }
    }

    private fun setRecyclerView() {
        lifecycleScope.launch {
            viewModel.archivedClassRoomFirestoreRecyclerOptions().collectLatest {
                when (it) {
                    is Response.Loading -> {
                        binding.progressBar.visible()
                    }

                    is Response.Success -> {
                        binding.progressBar.gone()
                        try {
                            adapter = it.data?.let { it1 ->
                                ClassRoomAdapter(
                                    it1, ::classRoomAdapterViewController
                                )
                            }!!
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
        holder.binding.apply {
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

    private fun ifClassRoomListEmptyShowEmpty() {
        lifecycleScope.launch {
            viewModel.isArchiveClassListEmpty().collectLatest {
                binding.apply {
                    tvNotFound.isVisible = it
                    ivNotFound.isVisible = it
                    recyclerView.isVisible = !it
                }
            }
        }
    }

}