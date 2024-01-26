package com.kivous.attendanceroom.ui.fragments.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kivous.attendanceroom.R
import com.kivous.attendanceroom.check_network_connectivity.NetworkViewModel
import com.kivous.attendanceroom.data.models.User
import com.kivous.attendanceroom.databinding.FragmentResponseListBinding
import com.kivous.attendanceroom.ui.adapters.ResponseAdapter
import com.kivous.attendanceroom.ui.viewmodels.SharedViewModel
import com.kivous.attendanceroom.ui.viewmodels.teacher.ResponseListViewModel
import com.kivous.attendanceroom.utils.Constant.ATTENDANCE_ID
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.glideCircle
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.logD
import com.kivous.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ResponseListFragment : Fragment() {
    private var _binding: FragmentResponseListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ResponseListViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var adapter: ResponseAdapter
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var attendanceId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResponseListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        attendanceId = arguments?.getString(ATTENDANCE_ID).toString()

        binding.vClose.setOnClickListener {
            findNavController().navigateUp()
        }

        observeNetworkConnectionAndHandleUI()

        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { code ->
            setRecyclerView(code)
            updateUIBasedOnResponseCount(code)
        }

    }

    override fun onStart() {
        super.onStart()
        bottomNav = requireActivity().findViewById(R.id.bottom_navigation)
        bottomNav.gone()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun observeNetworkConnectionAndHandleUI() {
        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest {
                binding.viewNetworkError.isVisible = !it
            }
        }
    }

    private fun setRecyclerView(code: String) {
        lifecycleScope.launch {
            viewModel.getResponseFirestoreRecyclerOptions(code, attendanceId).collectLatest {
                when (it) {
                    is Response.Loading -> {
                    }

                    is Response.Success -> {
                        try {
                            adapter = it.data?.let { it1 ->
                                ResponseAdapter(
                                    it1, ::responseAdapterViewController
                                )
                            }!!
                        } catch (e: Exception) {
                            logD(e.message.toString())
                        }
                        binding.recyclerView.adapter = adapter
                        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
                        adapter.startListening()
                    }

                    is Response.Error -> {
                    }
                }
            }
        }
    }

    private fun responseAdapterViewController(
        holder: ResponseAdapter.ViewHolder, model: User
    ) {
        holder.binding.apply {
            ivPhoto.glideCircle(requireContext(), model.photoUrl)
            tvName.text = model.name.toString()
            tvEmail.text = model.email.toString()
        }
    }

    private fun updateUIBasedOnResponseCount(code: String) {
        lifecycleScope.launch {
            viewModel.getResponseCount(code, attendanceId).collectLatest {
                if (it == 0) {
                    binding.apply {
                        recyclerView.gone()
                        tvNoResponses.visible()
                        tvCount.gone()
                    }
                } else {
                    binding.apply {
                        recyclerView.visible()
                        tvNoResponses.gone()
                        tvCount.visible()
                        tvCount.text = it.toString()
                    }
                }
            }
        }
    }


}