package com.kivous.attendanceroom.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.kivous.attendanceroom.ui.viewmodels.AppViewModel
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
    private val viewModel: AppViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: AppViewModel by activityViewModels()
    private lateinit var adapter: ResponseAdapter
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResponseListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val attendanceId = arguments?.getString(ATTENDANCE_ID).toString()
        binding.vClose.setOnClickListener {
            findNavController().navigateUp()
        }
        whenNoInternet()
        setUpRecyclerView()
        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { code ->
            viewModel.getResponseFirestoreRecyclerOptions(code, attendanceId)
            viewModel.getResponseCount(code, attendanceId)
        }
        whenNoResponses()
    }

    private fun whenNoResponses() {
        lifecycleScope.launch {
            viewModel.responseCount.collectLatest {
                if (it == 0) {
                    binding.tvNoResponses.visible()
                    binding.tvCount.gone()
                } else {
                    binding.tvNoResponses.gone()
                    binding.tvCount.visible()
                    binding.tvCount.text = it.toString()
                }
            }
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

    private fun whenNoInternet() {
        lifecycleScope.launch {
            networkViewModel.isConnected.collectLatest {
                if (it) {
                    binding.viewNetworkError.gone()
                } else {
                    binding.viewNetworkError.visible()
                }
            }
        }
    }

    private fun setUpRecyclerView() {
        lifecycleScope.launch {
            viewModel.responseFirestoreRecyclerOptions.collectLatest {
                when (it) {
                    is Response.Loading -> {
                    }

                    is Response.Success -> {
                        try {
                            adapter = it.data?.let { it1 -> ResponseAdapter(it1, ::responseAdapterViewController) }!!
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
        holder.apply {
            binding.apply {
                glideCircle(model.photoUrl, ivPhoto)
                binding.tvName.text = model.name.toString()
                binding.tvEmail.text = model.email.toString()
            }
        }

    }


}