package com.example.smartparkingsystem.ui.parkinglayout

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.gridlayout.widget.GridLayout
import com.example.smartparkingsystem.R
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

        val heightToWidthRatio = 1.8
        val spotWidth = dpToPx(60)
        val spotHeight = (spotWidth * heightToWidthRatio).toInt()

        // Spot ve road'ları kolayca bulmak için map'ler oluştur
        val spotMap = layout.parkingSpots.associateBy { it.row to it.column }
        val roadSet = layout.roads.map { it.roadRow to it.roadColumn }.toSet()

        for (r in 0 until layout.rows) {
            for (c in 0 until layout.columns) {
                val frameLayout = FrameLayout(requireContext())
                val border = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.WHITE)
                    setStroke(2, Color.BLACK)
                    cornerRadius = 4f
                }
                frameLayout.background = border

                val params = GridLayout.LayoutParams(
                    GridLayout.spec(r, 1, GridLayout.CENTER),
                    GridLayout.spec(c, 1, GridLayout.CENTER)
                ).apply {
                    width = spotWidth
                    height = spotHeight
                    setMargins(12, 12, 12, 12)
                }

                when {
                    roadSet.contains(r to c) -> {
                        // Road ise çizgi görselini göster
                        val lineImageView = ImageView(requireContext()).apply {
                            setImageResource(R.drawable.ic_line)
                            scaleType = ImageView.ScaleType.FIT_CENTER
                            // Görsel boyutunu ayarla
                            val padding = (spotHeight * 0.3).toInt()
                            setPadding(padding, padding, padding, padding)
                        }
                        frameLayout.addView(lineImageView)
                    }
                    spotMap.containsKey(r to c) -> {
                        // ParkingSpot ise
                        val spot = spotMap[r to c]!!

                        // spotIdentifier null ise row ve column'a göre otomatik oluştur
                        val identifier = if (spot.spotIdentifier.isNullOrEmpty()) {
                            // A1, B2 gibi sıra sütun gösterimi oluştur
                            val rowLetter = ('A' + r).toChar()
                            "$rowLetter${c+1}"
                        } else {
                            spot.spotIdentifier
                        }

                        val textView = TextView(requireContext()).apply {
                            text = identifier
                            gravity = Gravity.CENTER
                            setTextColor(Color.BLACK)
                            textSize = 14f
                        }
                        frameLayout.addView(textView)

                        if (spot.occupied) {
                            val carImageView = ImageView(requireContext()).apply {
                                setImageResource(R.drawable.ic_car)
                                scaleType = ImageView.ScaleType.FIT_CENTER
                                val padding = (spotHeight * 0.1).toInt()
                                setPadding(padding, padding, padding, padding)
                            }
                            frameLayout.addView(carImageView)
                        }
                    }
                    else -> {
                        // Ne road ne de spot ise boş bırak
                    }
                }

                grid.addView(frameLayout, params)
            }
        }
    }


    // DP'yi piksel değerine dönüştürmek için yardımcı fonksiyon
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }


    private fun showLoading(isLoading: Boolean) {
        binding.apply {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
