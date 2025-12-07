package com.souvikmondal01.attendanceroom.ui.fragments.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.souvikmondal01.attendanceroom.databinding.FragmentAttendanceBottomSheetBinding

class AttendanceBottomSheetFragment(private val bottomSheetViewController: (FragmentAttendanceBottomSheetBinding) -> Unit) :
    BottomSheetDialogFragment() {
    private var _binding: FragmentAttendanceBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomSheetViewController(binding)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        const val TAG = "AttendanceBottomSheetFragment"
    }

}