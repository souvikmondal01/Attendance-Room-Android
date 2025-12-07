package com.souvikmondal01.attendanceroom.ui.fragments.common

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.souvikmondal01.attendanceroom.R
import com.souvikmondal01.attendanceroom.databinding.FragmentPermissionBinding
import com.souvikmondal01.attendanceroom.utils.gone
import com.souvikmondal01.attendanceroom.utils.visible


class PermissionFragment : Fragment() {
    private var _binding: FragmentPermissionBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!hasLocationPermission()) {
            locationPermission()
        }

        binding.cvRefresh.setOnClickListener {
            if (!hasLocationPermission()) {
                showRequiredLocationPermissionDialogBox()
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (hasLocationPermission()) {
            findNavController().navigate(R.id.action_permissionFragment_to_homeFragment)
        }
    }

    override fun onStop() {
        super.onStop()
        binding.cvRefresh.visible()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun hasLocationPermission(): Boolean {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val result = ContextCompat.checkSelfPermission(requireContext(), permission)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun locationPermission() {

        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    findNavController().navigate(R.id.action_permissionFragment_to_homeFragment)
                }

                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    // Only approximate location access granted.
                    showRequiredLocationPermissionDialogBox()
                }

                else -> {
                    // No location access granted.
                    showRequiredLocationPermissionDialogBox()
                }
            }
        }

        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun showRequiredLocationPermissionDialogBox() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Location permission")
            .setMessage("You need to accept location permission to use this app.")
            .setCancelable(true)
        builder.setPositiveButton("Okay") { _, _ ->
            openSetting()
            binding.cvRefresh.gone()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
            binding.cvRefresh.visible()
        }
        val dialog = builder.create()
        dialog.setOnCancelListener {
            dialog.dismiss()
            binding.cvRefresh.visible()
        }
        dialog.show()
    }

    private fun openSetting() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val uri: Uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        startActivity(intent)
    }

}