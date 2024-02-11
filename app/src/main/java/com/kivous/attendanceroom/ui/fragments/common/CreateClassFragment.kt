package com.kivous.attendanceroom.ui.fragments.common

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.kivous.attendanceroom.check_network_connectivity.NetworkViewModel
import com.kivous.attendanceroom.data.models.ClassRoom
import com.kivous.attendanceroom.databinding.FragmentCreateClassBinding
import com.kivous.attendanceroom.ui.viewmodels.common.CreateClassViewModel
import com.kivous.attendanceroom.utils.Common
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.hideKeyboard
import com.kivous.attendanceroom.utils.snackBar
import com.kivous.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class CreateClassFragment : Fragment() {
    private var _binding: FragmentCreateClassBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CreateClassViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateClassBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vClose.setOnClickListener {
            findNavController().navigateUp()
        }

        viewModel.generateClassJoinCode()

        observeNetworkConnectionAndHandleUI()
        whenClassNameNotEmptyEnableCreateButton()
        createButtonOnClick()
        whenClassRoomCreated()

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

    private fun whenClassNameNotEmptyEnableCreateButton() {
        binding.etClassName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(
                s: CharSequence, start: Int, count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int, count: Int
            ) {
                binding.btnCreate.isEnabled = s.trim().isNotEmpty()
            }
        })
    }

    private fun createButtonOnClick() {
        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest { isConnected ->
                binding.btnCreate.setOnClickListener {
                    if (isConnected) {
                        val className = binding.etClassName.text.toString().trim()
                        val department = binding.etDepartment.text.toString().trim()
                        val batch = binding.etBatch.text.toString().trim()
                        val subject = binding.etSubject.text.toString().trim()

                        val email = auth.currentUser!!.email.toString()
                        val name = auth.currentUser!!.displayName.toString()
                        val photoUrl = auth.currentUser!!.photoUrl.toString()

                        lifecycleScope.launch(Dispatchers.IO) {
                            viewModel.classJoinCode.collectLatest { classJoinCode ->
                                val classRoom = ClassRoom(
                                    className = className,
                                    department = department,
                                    batch = batch,
                                    subject = subject,
                                    code = classJoinCode,
                                    creatorName = name,
                                    creatorEmail = email,
                                    creatorPhotoUrl = photoUrl,
                                    date = Date(),
                                    timestamp = Common.timeStamp(),
                                    teacherEmailList = arrayListOf(email),
                                    studentEmailList = arrayListOf(),
                                    participantEmailList = arrayListOf(email)
                                )
                                viewModel.createClassRoom(classRoom, classJoinCode)
                            }
                        }

                    } else {
                        hideKeyboard()
                        snackBar("Connect to internet to create the class")
                    }
                }
            }
        }

    }

    private fun whenClassRoomCreated() {
        lifecycleScope.launch {
            viewModel.classJoinCode.collectLatest { code ->
                viewModel.isClassRoomCreated.collectLatest {
                    when (it) {
                        is Response.Loading -> {
                            binding.apply {
                                btnCreate.isEnabled = false
                                etClassName.isEnabled = false
                                etDepartment.isEnabled = false
                                etSubject.isEnabled = false
                                etBatch.isEnabled = false
                                progressBar.visible()
                            }
                        }

                        is Response.Success -> {
                            binding.progressBar.gone()
                            findNavController().navigateUp()
                            viewModel.addCodeToFirestoreCodeList(code)
                        }

                        is Response.Error -> {
                            if (it.message?.isNotEmpty() == true) {
                                binding.apply {
                                    btnCreate.isEnabled = true
                                    etClassName.isEnabled = true
                                    etDepartment.isEnabled = true
                                    etSubject.isEnabled = true
                                    etBatch.isEnabled = true
                                    progressBar.gone()
                                }
                            }

                        }
                    }
                }

            }
        }
    }

}