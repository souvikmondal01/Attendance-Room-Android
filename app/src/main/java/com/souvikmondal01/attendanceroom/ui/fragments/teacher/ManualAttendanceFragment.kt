package com.souvikmondal01.attendanceroom.ui.fragments.teacher

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
import com.souvikmondal01.attendanceroom.R
import com.souvikmondal01.attendanceroom.check_network_connectivity.NetworkViewModel
import com.souvikmondal01.attendanceroom.data.models.Attendance
import com.souvikmondal01.attendanceroom.data.models.User
import com.souvikmondal01.attendanceroom.databinding.FragmentManualAttendanceBinding
import com.souvikmondal01.attendanceroom.ui.adapters.ManualAttendanceAdapter
import com.souvikmondal01.attendanceroom.ui.viewmodels.SharedViewModel
import com.souvikmondal01.attendanceroom.ui.viewmodels.teacher.ManualAttendanceViewModel
import com.souvikmondal01.attendanceroom.utils.Common
import com.souvikmondal01.attendanceroom.utils.Common.datePicker
import com.souvikmondal01.attendanceroom.utils.Response
import com.souvikmondal01.attendanceroom.utils.glideCircle
import com.souvikmondal01.attendanceroom.utils.gone
import com.souvikmondal01.attendanceroom.utils.hideKeyboard
import com.souvikmondal01.attendanceroom.utils.snackBar
import com.souvikmondal01.attendanceroom.utils.toast
import com.souvikmondal01.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManualAttendanceFragment : Fragment() {
    private var _binding: FragmentManualAttendanceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ManualAttendanceViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var adapter: ManualAttendanceAdapter
    private lateinit var userList: ArrayList<User>
    private var doubleBackToExitPressedOnce = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManualAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeNetworkConnectionAndHandleUI()

        userList = arrayListOf()

        binding.apply {
            vClose.setOnClickListener {
                doubleBackPressToExit()
            }
            binding.etDate.setText(Common.date())
            binding.etDate.setOnClickListener {
                datePicker(binding.etDate)
            }
        }

        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { code ->
            setRecyclerView(code)
            doneButtonOnClick(code)
        }

        whenManualResponseSet()

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

    private fun observeNetworkConnectionAndHandleUI() {
        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest {
                if (!it) {
                    snackBar("No internet connection found")
                }
            }
        }
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
        }, 2000L)
    }

    private fun setRecyclerView(code: String) {
        lifecycleScope.launch {
            viewModel.getStudentList(code).collectLatest {
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

    private fun manualAttendanceAdapterViewController(
        holder: ManualAttendanceAdapter.ViewHolder, user: User
    ) {
        holder.binding.apply {
            tvName.text = user.name.toString()
            tvEmail.text = user.email.toString()
            ivPhoto.glideCircle(requireContext(), user.photoUrl.toString())
            checkbox.setOnClickListener {
                if (checkbox.isChecked) {
                    userList.add(user)
                } else {
                    userList.remove(user)
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
                                    code, Attendance(
                                        date = date, finalResponseList = userList, notes = notes
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

    private fun whenManualResponseSet() {
        lifecycleScope.launch {
            viewModel.isManualResponseSet.collectLatest {
                when (it) {
                    is Response.Loading -> {
                        binding.progressBar.visible()
                        binding.btnDone.isEnabled = false
                    }

                    is Response.Success -> {
                        binding.progressBar.gone()
                        binding.btnDone.isEnabled = true
                        findNavController().navigateUp()
                    }

                    is Response.Error -> {
                        binding.btnDone.isEnabled = true
                        binding.progressBar.gone()
                    }
                }
            }
        }
    }


}