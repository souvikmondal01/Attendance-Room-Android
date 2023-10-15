package com.kivous.attendanceroom.ui.fragments

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
import com.kivous.attendanceroom.check_network_connectivity.NetworkViewModel
import com.kivous.attendanceroom.data.models.ClassRoom
import com.kivous.attendanceroom.databinding.FragmentCreateClassBinding
import com.kivous.attendanceroom.ui.viewmodels.AppViewModel
import com.kivous.attendanceroom.utils.Constant.FAILURE_CODE
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.hideKeyboard
import com.kivous.attendanceroom.utils.snackBar
import com.kivous.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class CreateClassFragment : Fragment() {
    private var _binding: FragmentCreateClassBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AppViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()

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
        whenNoInternet()
        viewModel.generateClassJoinCode()
        createButtonOnClick()
        whenClassRoomCreated()
        whenClassNameNotEmptyEnableCreateButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
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

    private fun whenClassRoomCreated() {
        lifecycleScope.launch {
            viewModel.code.collectLatest { code ->
                viewModel.isClassRoomCreated.collectLatest {
                    when (it) {
                        is Response.Loading -> {
                            binding.btnCreate.isEnabled = false
                            binding.etClassName.isEnabled = false
                            binding.etDepartment.isEnabled = false
                            binding.etSubject.isEnabled = false
                            binding.etBatch.isEnabled = false
                            binding.progressBar.visible()
                        }

                        is Response.Success -> {
                            binding.progressBar.gone()
                            findNavController().navigateUp()
                            viewModel.addCodeToFirestoreCodeList(code)
                        }

                        is Response.Error -> {
                            if (it.message.toString() == FAILURE_CODE) {
                                binding.progressBar.gone()
                                binding.btnCreate.isEnabled = true
                                binding.etClassName.isEnabled = true
                                binding.etDepartment.isEnabled = true
                                binding.etSubject.isEnabled = true
                                binding.etBatch.isEnabled = true
                            }

                        }
                    }
                }

            }
        }
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

                        lifecycleScope.launch {
                            viewModel.code.collectLatest { code ->
                                val classRoom = ClassRoom(
                                    className = className,
                                    department = department,
                                    batch = batch,
                                    subject = subject,
                                    code = code,
                                )
                                viewModel.createClassRoom(classRoom, code)
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