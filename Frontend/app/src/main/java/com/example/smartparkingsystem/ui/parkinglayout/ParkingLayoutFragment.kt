package com.example.smartparkingsystem.ui.parkinglayout

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.gridlayout.widget.GridLayout
import com.example.smartparkingsystem.data.model.ParkingLayoutResponse
import com.example.smartparkingsystem.databinding.FragmentParkingLayoutBinding
import com.example.smartparkingsystem.utils.state.UiState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ParkingLayoutFragment : Fragment() {

    private var _binding: FragmentParkingLayoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ParkingLayoutViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentParkingLayoutBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parkingId = arguments?.getInt("parkingId")
        observeParkingLayout(parkingId ?: 0)
    }

    private fun observeParkingLayout(parkingId: Int) {
        viewModel.getParkingLayout(parkingId)
        viewModel.layout.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> showLoading(true)
                is UiState.Success -> {
                    showLoading(false)
                    drawLayout(state.data)
                }

                is UiState.Error -> {
                    showLoading(false)
                    Snackbar.make(
                        binding.root,
                        state.message ?: "Error loading parking layout",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun drawLayout(layout: ParkingLayoutResponse) {
        val grid = binding.gridLayout
        grid.columnCount = layout.columns
        grid.rowCount = layout.rows
        grid.removeAllViews()

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val horizontalSpace = (screenWidth - (layout.columns + 1) * 16) / layout.columns
        val verticalSpace = (screenHeight - (layout.rows + 1) * 16) / layout.rows

        val spotSize = minOf(horizontalSpace, verticalSpace)

        layout.spots.forEach { spot ->
            val textView = TextView(requireContext()).apply {
                text = spot.spotIdentifier
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setBackgroundColor(
                    if (spot.occupied) Color.GREEN else Color.RED
                )
            }

            val params = GridLayout.LayoutParams(
                GridLayout.spec(spot.row),
                GridLayout.spec(spot.column)
            ).apply {
                width = spotSize
                height = spotSize
                setMargins(8, 8, 8, 8)
            }

            grid.addView(textView, params)
        }
    }



    private fun showLoading(isLoading: Boolean) {
        binding.apply {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

}