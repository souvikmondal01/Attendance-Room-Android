package com.kivous.attendanceroom.ui.fragments.common

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.kivous.attendanceroom.R
import com.kivous.attendanceroom.check_network_connectivity.NetworkViewModel
import com.kivous.attendanceroom.data.models.Chat
import com.kivous.attendanceroom.databinding.FragmentCommunicateBinding
import com.kivous.attendanceroom.ui.adapters.ChatAdapter
import com.kivous.attendanceroom.ui.viewmodels.SharedViewModel
import com.kivous.attendanceroom.ui.viewmodels.common.CommunicateViewModel
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.glideCircle
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.setImageViewTint
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class CommunicateFragment : Fragment() {
    private var _binding: FragmentCommunicateBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CommunicateViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var adapter: ChatAdapter

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommunicateBinding.inflate(inflater, container, false)
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            vBackArrow.setOnClickListener {
                recyclerView.stopScroll().let {
                    findNavController().navigateUp()
                }
            }
        }

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                binding.recyclerView.stopScroll().let {
                    findNavController().navigateUp()
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) { code ->
            viewModel.getClassRoomDetails(code)
            viewModel.getChatFirestoreRecyclerOptions(code)
            binding.ivSend.setOnClickListener {
                val message = binding.etMessage.text.toString().trim()
                if (message.isNotEmpty()) {
                    viewModel.setChatToFirestore(code, Chat(message = message))
                    binding.etMessage.text?.clear()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.classRoomDetails.collectLatest {
                binding.tvTitle.text = it.data?.className
            }
        }

        binding.ivProfile.glideCircle(requireContext(), auth.currentUser!!.photoUrl.toString())
        setRecyclerView()

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {

                if (p0.toString().trim().isNotEmpty()) {
                    setImageViewTint(
                        binding.ivSend, com.google.android.material.R.attr.colorOnSurface
                    )
                } else {
                    setImageViewTint(
                        binding.ivSend, com.google.android.material.R.attr.colorOutline
                    )
                }
            }

            override fun afterTextChanged(p0: Editable?) {
            }
        })

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

    private fun setRecyclerView() {
        lifecycleScope.launch {
            viewModel.chatFirestoreRecyclerOptions.collectLatest {
                when (it) {
                    is Response.Loading -> {

                    }

                    is Response.Success -> {
                        adapter = it.data?.let { it1 ->
                            ChatAdapter(
                                auth, requireContext(), it1, ::chatAdapterViewController
                            )
                        }!!
                        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
                        binding.recyclerView.adapter = adapter

                        adapter.registerAdapterDataObserver(object :
                            RecyclerView.AdapterDataObserver() {
                            override fun onItemRangeInserted(
                                positionStart: Int, itemCount: Int
                            ) {
                                super.onItemRangeInserted(positionStart, itemCount)
                                val lastVisiblePosition =
                                    LinearLayoutManager(requireContext()).findLastCompletelyVisibleItemPosition()
                                if (lastVisiblePosition == -1 || positionStart >= adapter.itemCount - 1 && lastVisiblePosition == positionStart - 1) {
                                    binding.recyclerView.scrollToPosition(positionStart)
                                }
                            }
                        })

                        adapter.startListening()
                    }

                    is Response.Error -> {

                    }
                }
            }
        }
    }

    private fun chatAdapterViewController(holder: ChatAdapter.ViewHolder, model: Chat) {

        holder.apply {
            tvMessage.text = model.message.toString()
            tvName.text = model.name.toString()
            tvCurrentDate.text = model.currentDate.toString()
            tvCurrentTime.text = model.currentTime.toString()
            ivProfile.glideCircle(requireContext(), model.photoUrl.toString())

            if (model.email == auth.currentUser!!.email) {
                itemView.setOnLongClickListener {
                    MaterialAlertDialogBuilder(requireContext()).setTitle("Want to delete?")
                        .setPositiveButton("Delete") { _, _ ->
                            sharedViewModel.sharedClassCode.observe(
                                viewLifecycleOwner
                            ) {
                                viewModel.deleteChat(it, model.id.toString())
                            }
                        }.setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }.setCancelable(true).show()
                    true
                }
            }
        }
    }


}