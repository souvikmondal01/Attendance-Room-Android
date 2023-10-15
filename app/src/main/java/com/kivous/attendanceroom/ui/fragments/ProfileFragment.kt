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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.kivous.attendanceroom.R
import com.kivous.attendanceroom.check_network_connectivity.NetworkViewModel
import com.kivous.attendanceroom.databinding.FragmentProfileBinding
import com.kivous.attendanceroom.utils.glideCircle
import com.kivous.attendanceroom.utils.snackBar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var googleSignInClient: GoogleSignInClient
    private val networkViewModel: NetworkViewModel by viewModels()

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvName.text = auth.currentUser?.displayName.toString()
        binding.tvEmail.text = auth.currentUser?.email.toString()
        glideCircle(auth.currentUser?.photoUrl.toString(), binding.ivProfile)
        binding.vBackArrow.setOnClickListener {
            findNavController().navigateUp()
        }
        whenNoInternet()

        binding.tvSignOut.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).setTitle("Sign out")
                .setMessage("are you sure you want to leave?").setPositiveButton("Yes") { _, _ ->
                    auth.signOut()
                    googleSignOut()
                    findNavController().navigate(R.id.action_profileFragment_to_authFragment)
                }.setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }.setCancelable(true).show()
        }

        binding.cvThreeDot.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), binding.cvThreeDot)
            popupMenu.menuInflater.inflate(R.menu.popup_menu_one_option, popupMenu.menu)
            popupMenu.menu.findItem(R.id.one).title = "Archived classes"
            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.one -> {
                        findNavController().navigate(R.id.action_profileFragment_to_archivedClassesFragment)
                    }
                }
                true
            }
            popupMenu.show()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
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

    private fun googleSignOut() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        googleSignInClient.signOut()
    }
}