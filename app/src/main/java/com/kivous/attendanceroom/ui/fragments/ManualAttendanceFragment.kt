package com.kivous.attendanceroom.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
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
import com.kivous.attendanceroom.data.models.User
import com.kivous.attendanceroom.databinding.FragmentManualAttendanceBinding
import com.kivous.attendanceroom.ui.adapters.ManualAttendanceAdapter
import com.kivous.attendanceroom.ui.viewmodels.AppViewModel
import com.kivous.attendanceroom.utils.Common.date
import com.kivous.attendanceroom.utils.Common.datePicker
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.glideCircle
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.hideKeyboard
import com.kivous.attendanceroom.utils.snackBar
import com.kivous.attendanceroom.utils.toast
import com.kivous.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Suppress("NAME_SHADOWING")
@AndroidEntryPoint
class ManualAttendanceFragment : Fragment() {
    private var _binding: FragmentManualAttendanceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AppViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: AppViewModel by activityViewModels()
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var adapter: ManualAttendanceAdapter
    private lateinit var userList: ArrayList<User>
    private var doubleBackToExitPressedOnce = false
    private val doublePressTimeWindow = 2000L
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManualAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vClose.setOnClickListener {
            doubleBackPressToExit()
        }
        whenNoInternet()
        binding.etDate.setText(date())
        binding.etDate.setOnClickListener {
            datePicker(binding.etDate)
        }
        userList = arrayListOf()

        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { code ->
            viewModel.getStudentList(code)
            doneButtonOnClick(code)
        }
        whenManualResponseSet()
        setUpRecyclerView()

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                doubleBackPressToExit()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
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

    private fun doubleBackPressToExit() {
        if (doubleBackToExitPressedOnce) {
            findNavController().navigateUp()
            return
        }
        doubleBackToExitPressedOnce = true
        toast("Press again to exit")
        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToExitPressedOnce = false
        }, doublePressTimeWindow)
    }

    private fun setUpRecyclerView() {
        lifecycleScope.launch {
            viewModel.studentList.collectLatest {
                when (it) {
                    is Response.Loading -> {
                        binding.progressBarCircular.visible()
                    }

                    is Response.Success -> {
                        binding.progressBarCircular.gone()
                        adapter = ManualAttendanceAdapter(::manualAttendanceAdapterViewController)
                        adapter.submitList(it.data)
                        binding.recyclerView.setHasFixedSize(true)
                        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
                        binding.recyclerView.adapter = adapter

                        if (it.data?.isEmpty() == true) {
                            binding.tvNoStudents.visible()
                            binding.ivNotFound.visible()
                        }
                    }

                    is Response.Error -> {
                        binding.progressBarCircular.gone()
                    }
                }
            }
        }
    }

    private fun whenManualResponseSet() {
        lifecycleScope.launch {
            viewModel.isManualResponseSet.collectLatest {
                when (it) {
                    is Response.Loading -> {
                        binding.progressBar.visible()
                    }

                    is Response.Success -> {
                        binding.progressBar.gone()
                        findNavController().navigateUp()
                    }

                    is Response.Error -> {
                        binding.progressBar.gone()
                    }
                }
            }
        }
    }

    private fun doneButtonOnClick(code: String) {
        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest { isConnected ->
                binding.btnDone.setOnClickListener {
                    if (isConnected) {
                        val date = binding.etDate.text.toString().trim()
                        val notes = binding.etNotes.text.toString().trim()
                        MaterialAlertDialogBuilder(
                            requireContext()
                        ).setTitle("Done taking attendance?")
                            .setMessage("You can access all attendance details later in the 'Attendance History' section.")
                            .setPositiveButton("Done") { _, _ ->
                                viewModel.setManualResponseList(
                                    code,
                                    Attendance(
                                        date = date,
                                        finalResponseList = userList,
                                        notes = notes
                                    )
                                )

                            }.setNegativeButton("Cancel") { dialog, _ ->
                                dialog.dismiss()
                            }.setCancelable(true).show()
                    } else {
                        hideKeyboard()
                        snackBar("Something went wrong, check your internet connection and try again.")
                    }
                }
            }
        }
    }

    private fun manualAttendanceAdapterViewController(
        holder: ManualAttendanceAdapter.ViewHolder, user: User
    ) {
        holder.apply {
            binding.apply {
                tvName.text = user.name.toString()
                tvEmail.text = user.email.toString()
                glideCircle(user.photoUrl.toString(), ivPhoto)
                checkbox.setOnClickListener {
                    if (checkbox.isChecked) {
                        userList.add(user)
                    } else {
                        userList.remove(user)
                    }
                }

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