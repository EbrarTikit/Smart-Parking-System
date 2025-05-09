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

        // Sabit genişlik/yükseklik oranı tanımlayalım (dikdörtgen görünümü için)
        val heightToWidthRatio = 1.8 // Yükseklik genişliğin %60'ı olacak

        // Temel boyutlar için minimum hesaplama
        val spotWidth = dpToPx(60) // 80dp sabit genişlik
        val spotHeight = (spotWidth * heightToWidthRatio).toInt() // Orantılı yükseklik

        layout.spots.forEach { spot ->
            // Her spot için FrameLayout oluştur
            val frameLayout = FrameLayout(requireContext())

            // Kenarlık oluştur
            val border = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)  // Beyaz arka plan
                setStroke(2, Color.BLACK)  // Siyah kenarlık
                cornerRadius = 4f  // Hafif yuvarlatılmış köşeler
            }
            frameLayout.background = border

            // Spot tanımlayıcı metni
            val textView = TextView(requireContext()).apply {
                text = spot.spotIdentifier
                gravity = Gravity.CENTER
                setTextColor(Color.BLACK)
                textSize = 14f
            }
            frameLayout.addView(textView)

            // Eğer spot doluysa araç görseli ekle
            if (spot.occupied) {
                val carImageView = ImageView(requireContext()).apply {
                    setImageResource(R.drawable.ic_car)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    val padding = (spotHeight * 0.1).toInt()
                    setPadding(padding, padding, padding, padding)
                }
                frameLayout.addView(carImageView)
            }

            val params = GridLayout.LayoutParams(
                GridLayout.spec(spot.row, 1, GridLayout.CENTER),
                GridLayout.spec(spot.column, 1, GridLayout.CENTER)
            ).apply {
                width = spotWidth
                height = spotHeight
                setMargins(12, 12, 12, 12)
            }

            grid.addView(frameLayout, params)
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
