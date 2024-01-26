package com.kivous.attendanceroom.ui.fragments.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavHost
import androidx.navigation.ui.setupWithNavController
import com.kivous.attendanceroom.R
import com.kivous.attendanceroom.databinding.FragmentTeacherRoomBinding
import com.kivous.attendanceroom.utils.logD

class TeacherRoomFragment : Fragment() {
    private var _binding: FragmentTeacherRoomBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeacherRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            setBottomNavigation()
        } catch (e: Exception) {
            logD(e.message.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun setBottomNavigation() {
        val navHost = childFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHost
        val navController = navHost.navController
        binding.bottomNavigation.setupWithNavController(navController)
    }

}