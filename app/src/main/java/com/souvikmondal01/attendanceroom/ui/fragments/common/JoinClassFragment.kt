package com.souvikmondal01.attendanceroom.ui.fragments.common

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
import com.souvikmondal01.attendanceroom.check_network_connectivity.NetworkViewModel
import com.souvikmondal01.attendanceroom.databinding.FragmentJoinClassBinding
import com.souvikmondal01.attendanceroom.ui.viewmodels.common.JoinClassViewModel
import com.souvikmondal01.attendanceroom.utils.Constant.ALREADY_STUDENT
import com.souvikmondal01.attendanceroom.utils.Constant.ALREADY_TEACHER
import com.souvikmondal01.attendanceroom.utils.Constant.CLASS_JOIN_DISABLE
import com.souvikmondal01.attendanceroom.utils.Constant.CODE_NOT_EXIST
import com.souvikmondal01.attendanceroom.utils.Response
import com.souvikmondal01.attendanceroom.utils.glideCircle
import com.souvikmondal01.attendanceroom.utils.gone
import com.souvikmondal01.attendanceroom.utils.hideKeyboard
import com.souvikmondal01.attendanceroom.utils.logD
import com.souvikmondal01.attendanceroom.utils.snackBar
import com.souvikmondal01.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class JoinClassFragment : Fragment() {
    private var _binding: FragmentJoinClassBinding? = null
    private val binding get() = _binding!!
    private val viewModel: JoinClassViewModel by viewModels()
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

        val userName = auth.currentUser?.displayName.toString()
        val userEmail = auth.currentUser?.email.toString()
        val userPhotoUrl = auth.currentUser?.photoUrl.toString()

        binding.apply {
            tvName.text = userName
            tvEmail.text = userEmail
            ivProfile.glideCircle(requireContext(), userPhotoUrl)

            cvClose.setOnClickListener {
                findNavController().navigateUp()
            }
        }

        observeNetworkConnectionAndHandleUI()
        whenCodeLengthValidEnableJoinButton()
        joinButtonOnClick()
        whenJoinedToClassWithCode()

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

    private fun whenCodeLengthValidEnableJoinButton() {
        binding.etClassCode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int, count: Int
            ) {
                binding.btnJoin.isEnabled = s.trim().toString().length == 6
            }
        })
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
                        binding.btnJoin.isEnabled = it.message.toString().isNotEmpty()

                        when (it.message) {
                            CODE_NOT_EXIST -> {
                                materialAlertDialog(
                                    "Class not found", "No class with that class code."
                                )
                            }

                            ALREADY_TEACHER -> {
                                materialAlertDialog(
                                    "Already joined", "You are the class teacher for that class."
                                )
                            }

                            ALREADY_STUDENT -> {
                                materialAlertDialog(
                                    "Already joined", "You have already joined the class."
                                )
                            }

                            CLASS_JOIN_DISABLE -> {
                                logD(CLASS_JOIN_DISABLE)
                                materialAlertDialog(
                                    "Can't join",
                                    "Class join option is disabled, contact your teacher."
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun materialAlertDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(
            requireContext()
        ).setTitle(title).setMessage(message).setNegativeButton("Dismiss") { dialog, _ ->
            dialog.dismiss()
        }.setCancelable(true).show()
    }

}