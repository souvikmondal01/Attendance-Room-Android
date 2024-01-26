package com.kivous.attendanceroom.ui.fragments.teacher

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
import com.kivous.attendanceroom.databinding.FragmentFinalResponseListBinding
import com.kivous.attendanceroom.ui.adapters.FinalResponseAdapter
import com.kivous.attendanceroom.ui.viewmodels.SharedViewModel
import com.kivous.attendanceroom.ui.viewmodels.teacher.FinalResponseListViewModel
import com.kivous.attendanceroom.utils.Constant.ATTENDANCE_ID
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.glideCircle
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.snackBar
import com.kivous.attendanceroom.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FinalResponseListFragment : Fragment() {
    private var _binding: FragmentFinalResponseListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FinalResponseListViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var adapter: FinalResponseAdapter
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var attendanceId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFinalResponseListBinding.inflate(inflater, container, false)
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
        }

    }

    override fun onStart() {
        super.onStart()
        bottomNav = requireActivity().findViewById(R.id.bottom_navigation)
        bottomNav.gone()
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

    private fun setRecyclerView(code: String) {
        lifecycleScope.launch {
            viewModel.getFinalResponseList(code, attendanceId).collectLatest {
                when (it) {
                    is Response.Loading -> {
                        binding.progressBar.visible()
                    }

                    is Response.Success -> {
                        binding.progressBar.gone()
                        binding.recyclerView.visible()
                        adapter = FinalResponseAdapter(::finalResponseAdapterViewController)
                        binding.recyclerView.setHasFixedSize(true)
                        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
                        binding.recyclerView.adapter = adapter
                        adapter.submitList(it.data)
                        binding.tvCount.text = it.data?.size.toString()
                        if (it.data?.size == 0) {
                            binding.tvNoResponses.visible()
                            binding.recyclerView.gone()
                        }
                    }

                    is Response.Error -> {
                        binding.progressBar.gone()
                    }
                }
            }
        }

    }

    private fun finalResponseAdapterViewController(
        holder: FinalResponseAdapter.ViewHolder, user: User
    ) {
        holder.binding.apply {
            ivPhoto.glideCircle(requireContext(), user.photoUrl.toString())
            tvName.text = user.name.toString()
            tvEmail.text = user.email.toString()
        }

    }

}