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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.kivous.attendanceroom.check_network_connectivity.NetworkViewModel
import com.kivous.attendanceroom.databinding.FragmentJoinClassBinding
import com.kivous.attendanceroom.ui.viewmodels.AppViewModel
import com.kivous.attendanceroom.utils.Constant.ALREADY_STUDENT
import com.kivous.attendanceroom.utils.Constant.ALREADY_TEACHER
import com.kivous.attendanceroom.utils.Constant.CLASS_JOIN_DISABLE
import com.kivous.attendanceroom.utils.Constant.CODE_NOT_EXIST
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.glideCircle
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.hideKeyboard
import com.kivous.attendanceroom.utils.logD
import com.kivous.attendanceroom.utils.snackBar
import com.kivous.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class JoinClassFragment : Fragment() {
    private var _binding: FragmentJoinClassBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AppViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJoinClassBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userEmail = auth.currentUser?.email.toString()
        val userName = auth.currentUser?.displayName.toString()
        val userPhotoUrl = auth.currentUser?.photoUrl.toString()
        binding.tvName.text = userName
        binding.tvEmail.text = userEmail
        glideCircle(userPhotoUrl, binding.ivProfile)
        binding.vClose.setOnClickListener {
            findNavController().navigateUp()
        }
        whenNoInternet()
        whenCodeLengthValidEnableJoinButton()
        joinButtonOnClick()
        whenJoinedToClassWithCode()

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun whenJoinedToClassWithCode() {
        lifecycleScope.launch {
            viewModel.isJoinedToClassWithCode.collectLatest {
                when (it) {
                    is Response.Loading -> {
                        binding.pb.visible()
                        binding.btnJoin.isEnabled = false
                    }

                    is Response.Success -> {
                        binding.pb.gone()
                        binding.btnJoin.isEnabled = true
                        findNavController().navigateUp()
                    }

                    is Response.Error -> {
                        binding.pb.gone()
                        if (it.message.toString().isNotEmpty()) {
                            binding.btnJoin.isEnabled = true
                        }
                        if (it.message.toString() == CODE_NOT_EXIST) {
                            MaterialAlertDialogBuilder(
                                requireContext()
                            ).setTitle("Class not found")
                                .setMessage("No class with that class code.")
                                .setNegativeButton("Dismiss") { dialog, _ ->
                                    dialog.dismiss()
                                }.setCancelable(true).show()
                        }
                        if (it.message.toString() == ALREADY_TEACHER) {
                            MaterialAlertDialogBuilder(
                                requireContext()
                            ).setTitle("Already joined")
                                .setMessage("You are the class teacher for that class.")
                                .setNegativeButton("Dismiss") { dialog, _ ->
                                    dialog.dismiss()
                                }.setCancelable(true).show()
                        }
                        if (it.message.toString() == ALREADY_STUDENT) {
                            MaterialAlertDialogBuilder(
                                requireContext()
                            ).setTitle("Already joined")
                                .setMessage("You have already joined the class.")
                                .setNegativeButton("Dismiss") { dialog, _ ->
                                    dialog.dismiss()
                                }.setCancelable(true).show()
                        }
                        if (it.message.toString() == CLASS_JOIN_DISABLE) {
                            logD(CLASS_JOIN_DISABLE)
                            MaterialAlertDialogBuilder(
                                requireContext()
                            ).setTitle("Can't join")
                                .setMessage("Class join option is disabled, contact your teacher.")
                                .setNegativeButton("Dismiss") { dialog, _ ->
                                    dialog.dismiss()
                                }.setCancelable(true).show()
                        }
                    }
                }
            }
        }
    }

    private fun joinButtonOnClick() {
        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest { isConnected ->
                binding.btnJoin.setOnClickListener {
                    val code = binding.etClassCode.text.toString()
                    if (isConnected) {
                        viewModel.joinClassWithCode(code)
                    } else {
                        hideKeyboard()
                        snackBar("Connect to internet to join the class")
                    }

                }
            }
        }
    }

    private fun whenCodeLengthValidEnableJoinButton() {
        binding.etClassCode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(
                s: CharSequence, start: Int, count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int, count: Int
            ) {
                binding.btnJoin.isEnabled = s.trim().toString().length == 6

            }
        })
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