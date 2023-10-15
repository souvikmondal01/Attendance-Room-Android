package com.kivous.attendanceroom.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kivous.attendanceroom.databinding.FragmentHomeBottomSheetBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlin.reflect.KFunction1

@AndroidEntryPoint
class HomeBottomSheetFragment(private val bottomSheetViewController: KFunction1<FragmentHomeBottomSheetBinding, Unit>) :
    BottomSheetDialogFragment() {
    private var _binding: FragmentHomeBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding =
            FragmentHomeBottomSheetBinding.inflate(inflater, container, false)
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
        const val TAG = "BottomSheetFragment"
    }

}
