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
import com.google.firebase.auth.FirebaseAuth
import com.kivous.attendanceroom.check_network_connectivity.NetworkViewModel
import com.kivous.attendanceroom.databinding.FragmentEditClassBinding
import com.kivous.attendanceroom.ui.viewmodels.AppViewModel
import com.kivous.attendanceroom.utils.Constant
import com.kivous.attendanceroom.utils.Constant.BATCH
import com.kivous.attendanceroom.utils.Constant.CLASS_NAME
import com.kivous.attendanceroom.utils.Constant.DEPARTMENT
import com.kivous.attendanceroom.utils.Constant.SUBJECT
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.hideKeyboard
import com.kivous.attendanceroom.utils.snackBar
import com.kivous.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EditClassFragment : Fragment() {
    private var _binding: FragmentEditClassBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AppViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditClassBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val code = arguments?.getString(Constant.CODE).toString()
        binding.vClose.setOnClickListener {
            findNavController().navigateUp()
        }
        whenNoInternet()
        viewModel.getClassRoomDetails(code)
        onSaveButtonClicked(code)
        whenClassRoomEdited()
        whenCurrentDataNotEqualToPreviousDataEnableSaveButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun whenClassRoomEdited() {
        lifecycleScope.launch {
            viewModel.isClassRoomEdited.collectLatest { res ->
                when (res) {
                    is Response.Loading -> {
                        binding.btnSave.isEnabled = false
                        binding.etClassName.isEnabled = false
                        binding.etDepartment.isEnabled = false
                        binding.etSubject.isEnabled = false
                        binding.etBatch.isEnabled = false
                        binding.progressBar.visible()
                    }

                    is Response.Success -> {
                        findNavController().navigateUp()
                        binding.progressBar.gone()
                    }

                    is Response.Error -> {
                        if (res.message.toString().isNotEmpty()) {
                            binding.progressBar.gone()
                            binding.btnSave.isEnabled = true
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

    private fun onSaveButtonClicked(code: String) {
        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest { isConnected ->
                binding.btnSave.setOnClickListener {
                    if (isConnected) {
                        val className = binding.etClassName.text.toString().trim()
                        val department = binding.etDepartment.text.toString().trim()
                        val batch = binding.etBatch.text.toString().trim()
                        val subject = binding.etSubject.text.toString().trim()

                        val map = mapOf(
                            CLASS_NAME to className,
                            DEPARTMENT to department,
                            BATCH to batch,
                            SUBJECT to subject
                        )

                        viewModel.editClassroomDetails(map, code)

                    } else {
                        hideKeyboard()
                        snackBar("Something went wrong, check your internet connection and try again.")
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

    private fun whenCurrentDataNotEqualToPreviousDataEnableSaveButton() {
        lifecycleScope.launch {
            viewModel.classRoomDetails.collectLatest {
                when (it) {
                    is Response.Loading -> {
                        binding.progressBar.visible()
                    }

                    is Response.Success -> {
                        val className = it.data?.className.toString()
                        val department = it.data?.department.toString()
                        val batch = it.data?.batch.toString()
                        val subject = it.data?.subject.toString()
                        binding.progressBar.gone()

                        viewModel.classNameTemp.value = className
                        viewModel.departmentTemp.value = department
                        viewModel.batchTemp.value = batch
                        viewModel.subjectTemp.value = subject

                        binding.etClassName.setText(className)
                        binding.etDepartment.setText(department)
                        binding.etBatch.setText(batch)
                        binding.etSubject.setText(subject)

                        binding.etClassName.addTextChangedListener(object : TextWatcher {
                            override fun afterTextChanged(s: Editable) {
                            }

                            override fun beforeTextChanged(
                                s: CharSequence, start: Int, count: Int, after: Int
                            ) {
                            }

                            override fun onTextChanged(
                                s1: CharSequence, start: Int, before: Int, count: Int
                            ) {
                                viewModel.classNameTemp.value = s1.toString().trim()
                                viewModel.departmentTemp.observe(viewLifecycleOwner) { d ->
                                    viewModel.batchTemp.observe(viewLifecycleOwner) { b ->
                                        viewModel.subjectTemp.observe(viewLifecycleOwner) { s ->
                                            binding.btnSave.isEnabled =
                                                (className != s1.toString()
                                                    .trim() || department != d || batch != b || subject != s) && s1.trim()
                                                    .isNotEmpty()
                                        }
                                    }
                                }
                            }
                        })

                        binding.etDepartment.addTextChangedListener(object : TextWatcher {
                            override fun afterTextChanged(s: Editable) {
                            }

                            override fun beforeTextChanged(
                                s: CharSequence, start: Int, count: Int, after: Int
                            ) {
                            }

                            override fun onTextChanged(
                                s2: CharSequence, start: Int, before: Int, count: Int
                            ) {
                                viewModel.departmentTemp.value = s2.toString().trim()
                                viewModel.classNameTemp.observe(viewLifecycleOwner) { cn ->
                                    viewModel.batchTemp.observe(viewLifecycleOwner) { b ->
                                        viewModel.subjectTemp.observe(viewLifecycleOwner) { s ->
                                            binding.btnSave.isEnabled =
                                                (department != s2.toString()
                                                    .trim() || className != cn || batch != b || subject != s) && (cn.isNotEmpty())
                                        }
                                    }
                                }
                            }
                        })


                        binding.etBatch.addTextChangedListener(object : TextWatcher {
                            override fun afterTextChanged(s: Editable) {
                            }

                            override fun beforeTextChanged(
                                s: CharSequence, start: Int, count: Int, after: Int
                            ) {
                            }

                            override fun onTextChanged(
                                s3: CharSequence, start: Int, before: Int, count: Int
                            ) {
                                viewModel.batchTemp.value = s3.toString().trim()
                                viewModel.classNameTemp.observe(viewLifecycleOwner) { cn ->
                                    viewModel.departmentTemp.observe(viewLifecycleOwner) { d ->
                                        viewModel.subjectTemp.observe(viewLifecycleOwner) { s ->
                                            binding.btnSave.isEnabled =
                                                (batch != s3.toString()
                                                    .trim() || className != cn || department != d || subject != s) && (cn.isNotEmpty())
                                        }
                                    }
                                }
                            }
                        })

                        binding.etSubject.addTextChangedListener(object : TextWatcher {
                            override fun afterTextChanged(s: Editable) {
                            }

                            override fun beforeTextChanged(
                                s: CharSequence, start: Int, count: Int, after: Int
                            ) {
                            }

                            override fun onTextChanged(
                                s4: CharSequence, start: Int, before: Int, count: Int
                            ) {
                                viewModel.subjectTemp.value = s4.toString().trim()
                                viewModel.classNameTemp.observe(viewLifecycleOwner) { cn ->
                                    viewModel.departmentTemp.observe(viewLifecycleOwner) { d ->
                                        viewModel.batchTemp.observe(viewLifecycleOwner) { b ->
                                            binding.btnSave.isEnabled =
                                                (subject != s4.toString()
                                                    .trim() || className != cn || department != d || batch != b) && (cn.isNotEmpty())
                                        }
                                    }
                                }
                            }
                        })
                    }

                    is Response.Error -> {
                        binding.progressBar.gone()
                    }
                }
            }
        }
    }

}