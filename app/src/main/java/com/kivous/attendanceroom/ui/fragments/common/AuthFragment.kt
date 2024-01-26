package com.kivous.attendanceroom.ui.fragments.common

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.kivous.attendanceroom.R
import com.kivous.attendanceroom.check_network_connectivity.NetworkViewModel
import com.kivous.attendanceroom.databinding.FragmentAuthBinding
import com.kivous.attendanceroom.ui.viewmodels.common.AuthViewModel
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.logD
import com.kivous.attendanceroom.utils.toast
import com.kivous.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class AuthFragment : Fragment() {
    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeNetworkConnectionAndHandleUI()
        updateUI(auth.currentUser)
        binding.btnGetStarted.setOnClickListener { googleSignIn() }
        whenUserDetailsSetToFirestore()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun observeNetworkConnectionAndHandleUI() {
        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest {
                binding.apply {
                    btnGetStarted.isEnabled = it
                    tvNoInternet.isVisible = !it
                    ivWarning.isVisible = !it
                }
            }
        }
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            findNavController().navigate(R.id.action_authFragment_to_permissionFragment)
        }
    }

    private fun googleSignIn() {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), googleSignInOptions)
        val signInIntent = googleSignInClient.signInIntent
        resultLauncher.launch(signInIntent)
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                if (task.isSuccessful) {
                    try {
                        val account = task.getResult(ApiException::class.java)
                        firebaseAuthWithGoogle(account.idToken!!)
                    } catch (e: ApiException) {
                        Toast.makeText(context, e.message.toString(), Toast.LENGTH_SHORT).show()
                        googleSignInClient.signOut()
                    }
                }
            }
        }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        binding.progressBar.visible()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                auth.signInWithCredential(firebaseCredential).await()
                viewModel.setUserDetailsToFirestore()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.gone()
                    toast(e.message ?: "Signin error!!")
                }
            }
        }
    }

    private fun whenUserDetailsSetToFirestore() {
        lifecycleScope.launch {
            viewModel.isUserSetToFirestore.collectLatest {
                when (it) {
                    is Response.Loading -> {
                        binding.progressBar.visible()
                    }

                    is Response.Success -> {
                        binding.progressBar.gone()
                        updateUI(auth.currentUser)
                    }

                    is Response.Error -> {
                        binding.progressBar.gone()
                    }
                }
            }
        }
    }

}