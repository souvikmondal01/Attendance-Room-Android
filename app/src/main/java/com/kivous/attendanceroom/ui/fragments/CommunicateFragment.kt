package com.kivous.attendanceroom.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.WindowManager
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
import com.kivous.attendanceroom.ui.viewmodels.AppViewModel
import com.kivous.attendanceroom.utils.Response
import com.kivous.attendanceroom.utils.glideCircle
import com.kivous.attendanceroom.utils.gone
import com.kivous.attendanceroom.utils.logD
import com.kivous.attendanceroom.utils.setImageViewTint
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class CommunicateFragment : Fragment() {
    private var _binding: FragmentCommunicateBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AppViewModel by viewModels()
    private val networkViewModel: NetworkViewModel by viewModels()
    private val sharedViewModel: AppViewModel by activityViewModels()
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
        binding.vBackArrow.setOnClickListener {
            findNavController().navigateUp()
        }

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

        glideCircle(auth.currentUser!!.photoUrl.toString(), binding.ivProfile)
        setUpRecyclerView()


        binding.etMessage.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                lifecycleScope.launch {
                    delay(300)
                    setUpRecyclerView()
                }

            }
        }

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

    private fun chatAdapterViewController(holder: ChatAdapter.ViewHolder, model: Chat) {
        holder.apply {
            tvMessage.text = model.message.toString()
            tvName.text = model.name.toString()
            tvCurrentDate.text = model.currentDate.toString()
            tvCurrentTime.text = model.currentTime.toString()
            glideCircle(model.photoUrl.toString(), ivProfile)

            if (model.email == auth.currentUser!!.email) {
                lifecycleScope.launch {
                    networkViewModel.isConnected.collectLatest { isConnected ->
                        itemView.setOnLongClickListener {
                            if (isConnected) {
                                MaterialAlertDialogBuilder(requireContext()).setTitle("Want to delete?")
                                    .setPositiveButton("Delete") { _, _ ->
                                        sharedViewModel.sharedClassCode.observe(viewLifecycleOwner) {
                                            viewModel.deleteChat(it, model.id.toString())
                                        }
                                    }.setNegativeButton("Cancel") { dialog, _ ->
                                        dialog.dismiss()
                                    }.setCancelable(true).show()
                            }
                            true
                        }

                    }
                }
            }
        }
    }

    private fun setUpRecyclerView() {
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

                        try {
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

                        } catch (e: Exception) {
                            logD(e.message.toString())
                        }

                        adapter.startListening()
                    }

                    is Response.Error -> {

                    }
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
        bottomNav.gone()
        adapter.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}