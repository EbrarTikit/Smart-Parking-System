package com.example.smartparkingsystem.ui.parkinglayout

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
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
import androidx.navigation.fragment.findNavController
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.data.model.ParkingLayoutResponse
import com.example.smartparkingsystem.data.model.SensorUpdateDto
import com.example.smartparkingsystem.data.remote.ParkingWebSocketClient
import com.example.smartparkingsystem.databinding.FragmentParkingLayoutBinding
import com.example.smartparkingsystem.utils.state.UiState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ParkingLayoutFragment : Fragment() {

    private var _binding: FragmentParkingLayoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ParkingLayoutViewModel by viewModels()

    private var webSocketClient: ParkingWebSocketClient? = null
    private val spotViews = mutableMapOf<Long, View>()

    private var totalSpotCount = 0
    private var occupiedSpotCount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("ParkingLayout", "onCreateView started")
        try {
            // Make sure binding is initialized properly
            _binding = FragmentParkingLayoutBinding.inflate(inflater, container, false)

            // Check if the GridLayout is accessible in the binding
            if (binding.gridLayout == null) {
                Log.e("ParkingLayout", "gridLayout is null in binding")
            } else {
                Log.d("ParkingLayout", "gridLayout found in binding")
            }

            // Similarly check other important views
            if (binding.btnBack == null) {
                Log.e("ParkingLayout", "btnBack is null in binding")
            }

            if (binding.tvParkingName == null) {
                Log.e("ParkingLayout", "tvParkingName is null in binding")
            }

            return binding.root
        } catch (e: Exception) {
            Log.e("ParkingLayout", "Error in onCreateView: ${e.message}", e)

            // Create a fallback view if binding fails
            val fallbackView = TextView(requireContext())
            fallbackView.text = "Failed to load parking layout: ${e.message}"
            fallbackView.gravity = Gravity.CENTER
            return fallbackView
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()

        val parkingId = arguments?.getInt("parkingId")
        observeParkingLayout(parkingId ?: 0)
        initializeWebSocket()
    }

    private fun setupToolbar() {
        try {
            binding.btnBack?.setOnClickListener {
                findNavController().navigateUp()
            } ?: Log.e("ParkingLayout", "Back button not found")
        } catch (e: Exception) {
            Log.e("ParkingLayout", "Error setting up toolbar: ${e.message}", e)
        }
    }

    private fun observeParkingLayout(parkingId: Int) {
        try {
            Log.d("ParkingLayout", "Loading layout for parking ID: $parkingId")
            viewModel.getParkingLayout(parkingId)
            viewModel.layout.observe(viewLifecycleOwner) { state ->
                when (state) {
                    is UiState.Loading -> {
                        Log.d("ParkingLayout", "Loading parking layout")
                        showLoading(true)
                    }

                    is UiState.Success -> {
                        Log.d(
                            "ParkingLayout",
                            "Parking layout loaded successfully: ${state.data.parkingName}"
                        )
                        showLoading(false)
                        drawLayout(state.data)
                    }

                    is UiState.Error -> {
                        Log.e("ParkingLayout", "Error loading parking layout: ${state.message}")
                        showLoading(false)
                        Snackbar.make(
                            binding.root,
                            state.message ?: "Error loading parking layout",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ParkingLayout", "Error observing layout: ${e.message}", e)
        }
    }

    private fun initializeWebSocket() {
        Log.d("ParkingLayout", "Initializing WebSocket connection...")
        webSocketClient = ParkingWebSocketClient { update ->
            Log.d("ParkingLayout", "Received WebSocket update - Spot ID: ${update.id}, Occupied: ${update.occupied}")
            activity?.runOnUiThread {
                updateParkingSpotStatus(update)
            }
        }

        // setConnectionLostTimeout metodu WebSocketClient'da mevcut
        webSocketClient?.setConnectionLostTimeout(0)
        
        try {
            webSocketClient?.connect()
            Log.d("ParkingLayout", "WebSocket connection initiated")
        } catch (e: Exception) {
            Log.e("ParkingLayout", "Error connecting to WebSocket: ${e.message}")
        }
    }

    private fun updateParkingSpotStatus(update: SensorUpdateDto) {
        Log.d("ParkingLayout", "Updating spot status - ID: ${update.id}, Occupied: ${update.occupied}")
        val view = spotViews[update.id]
        if (view == null) {
            Log.e("ParkingLayout", "No view found for spot ID: ${update.id}")
            return
        }
        
        Log.d("ParkingLayout", "Found view for spot ID: ${update.id}")
        val frameLayout = view as? FrameLayout
        if (frameLayout == null) {
            Log.e("ParkingLayout", "View is not a FrameLayout for spot ID: ${update.id}")
            return
        }

        activity?.runOnUiThread {
            // İstatistikleri güncellemek için spot durumunu kontrol et
            val currentViews = frameLayout.findViewWithTag<View>("car_image")
            val wasOccupied = currentViews != null
            
            // Eğer durum değiştiyse sayaçları güncelle
            if (wasOccupied != update.occupied) {
                if (update.occupied) {
                    occupiedSpotCount++
                } else {
                    occupiedSpotCount--
                }
                // İstatistikleri güncelle
                updateParkingStats()
            }
            
            // Find the spot identifier first
            val textView = frameLayout.findViewWithTag<TextView>("spotIdentifier") 
                ?: frameLayout.getChildAt(0) as? TextView
            
            // Check if this is a disabled spot by checking text or tag
            val isDisabled = textView?.text?.contains("Disabled", ignoreCase = true) ?: false
            
            // Update the background based on status and type
            val border = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                
                when {
                    isDisabled && update.occupied -> {
                        // Disabled + Occupied
                        colors = intArrayOf(
                            Color.parseColor("#6B8CFF"),
                            Color.parseColor("#4F69BC")
                        )
                    }
                    isDisabled -> {
                        // Disabled + Available
                        colors = intArrayOf(
                            Color.parseColor("#8BA3FF"),
                            Color.parseColor("#6B8CFF")
                        )
                    }
                    update.occupied -> {
                        // Regular + Occupied
                        colors = intArrayOf(
                            Color.parseColor("#FF7F7F"),
                            Color.parseColor("#FF5050")
                        )
                    }
                    else -> {
                        // Regular + Available
                        colors = intArrayOf(
                            Color.parseColor("#FFFFFF"),
                            Color.parseColor("#F7F7F7")
                        )
                    }
                }
                
                orientation = GradientDrawable.Orientation.TL_BR
                cornerRadius = 12f
                setStroke(3, Color.parseColor("#202020"))
            }
            
            frameLayout.background = border
            
            // Update text color
            textView?.setTextColor(if (update.occupied || isDisabled) Color.WHITE else Color.BLACK)
            
            // Remove all views except the identifier text
            val children = ArrayList<View>()
            for (i in 0 until frameLayout.childCount) {
                children.add(frameLayout.getChildAt(i))
            }
            
            val spotIdentifierText = children.filterIsInstance<TextView>().firstOrNull()
            frameLayout.removeAllViews()
            
            // Add back the identifier text
            if (spotIdentifierText != null) {
                frameLayout.addView(spotIdentifierText)
            }
            
            // Add car image if spot is occupied
            if (update.occupied) {
                val occupiedIndicator = View(requireContext()).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 8f
                        setColor(Color.parseColor("#FF5252"))
                    }
                    alpha = 0.2f
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
                frameLayout.addView(occupiedIndicator)

                val carImageView = ImageView(requireContext()).apply {
                    setImageResource(R.drawable.top_car)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    rotation = 90f

                    // Araç görselinin boyutlarını büyütün
                    val spotWidth = frameLayout.width
                    val spotHeight = frameLayout.height
                    
                    val width = (spotWidth * 0.75).toInt()   // Genişliği azalt
                    val height = (spotHeight * 0.9).toInt()  // Yüksekliği artır

                    layoutParams = FrameLayout.LayoutParams(
                        width,
                        height
                    ).apply {
                        gravity = Gravity.CENTER
                        setMargins(0, 0, 0, 0)
                    }

                    adjustViewBounds = true
                    background = null
                    tag = "car_image"
                }
                frameLayout.addView(carImageView)
                
                // Text'i yeniden öne getir
                if (textView != null) {
                    textView.bringToFront()
                }
            }
            
            // Add wheelchair icon for disabled spots
            if (isDisabled) {
                val wheelchairImageView = ImageView(requireContext()).apply {
                    setImageResource(R.drawable.ic_wheelchair)
                    scaleType = ImageView.ScaleType.FIT_XY
                    setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                    layoutParams = FrameLayout.LayoutParams(
                        dpToPx(24),
                        dpToPx(24)
                    ).apply {
                        gravity = Gravity.BOTTOM or Gravity.END
                        setMargins(0, 0, dpToPx(8), dpToPx(8))
                    }
                }
                frameLayout.addView(wheelchairImageView)
            }

            Log.d("ParkingLayout", "Successfully updated spot ID: ${update.id} to ${if (update.occupied) "occupied" else "available"}")
        }
    }

    override fun onDestroyView() {
        Log.d("ParkingLayout", "Destroying view and closing WebSocket connection")
        super.onDestroyView()
        webSocketClient?.close()
        webSocketClient = null
        spotViews.clear()
        _binding = null
        Log.d("ParkingLayout", "View destroyed and WebSocket connection closed")
    }

    private fun drawLayout(layout: ParkingLayoutResponse) {
        try {
            Log.d(
                "ParkingLayout",
                "Drawing layout: ${layout.parkingName}, rows: ${layout.rows}, cols: ${layout.columns}"
            )

            setupParkingStats(layout)

            val grid = binding.gridLayout
            grid.columnCount = layout.columns
            grid.rowCount = layout.rows
            grid.removeAllViews()
            
            // Clear previous spot views before redrawing
            spotViews.clear()

            val heightToWidthRatio = 2.0
            val spotWidth = dpToPx(70)
            val spotHeight = (spotWidth * heightToWidthRatio).toInt()

            val spotMap = layout.parkingSpots.associateBy { it.row to it.column }
            val roadSet = layout.roads.map { it.roadRow to it.roadColumn }.toSet()

            val horizontalRoads = mutableSetOf<Pair<Int, Int>>()
            val verticalRoads = mutableSetOf<Pair<Int, Int>>()

            for (road in layout.roads) {
                val pos = road.roadRow to road.roadColumn
                val leftPos = road.roadRow to (road.roadColumn - 1)
                val rightPos = road.roadRow to (road.roadColumn + 1)
                val topPos = (road.roadRow - 1) to road.roadColumn
                val bottomPos = (road.roadRow + 1) to road.roadColumn

                // Check adjacent cells to determine orientation
                if (roadSet.contains(leftPos) || roadSet.contains(rightPos)) {
                    horizontalRoads.add(pos)
                } else if (roadSet.contains(topPos) || roadSet.contains(bottomPos)) {
                    verticalRoads.add(pos)
                } else {
                    // If no adjacent roads, default to vertical
                    verticalRoads.add(pos)
                }
            }

            for (r in 0 until layout.rows) {
                for (c in 0 until layout.columns) {
                    val frameLayout = FrameLayout(requireContext())

                    val params = GridLayout.LayoutParams(
                        GridLayout.spec(r, 1, GridLayout.CENTER),
                        GridLayout.spec(c, 1, GridLayout.CENTER)
                    ).apply {
                        width = spotWidth
                        height = spotHeight
                        setMargins(8, 8, 8, 8)
                    }

                    when {
                        roadSet.contains(r to c) -> {
                            // Road styling
                            val roadBackground = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                setColor(Color.parseColor("#FFFFFF"))
                                cornerRadius = 4f
                            }
                            frameLayout.background = roadBackground

                            val lineImageView = ImageView(requireContext()).apply {
                                setImageResource(R.drawable.ic_line)
                                scaleType = ImageView.ScaleType.FIT_XY
                                setColorFilter(Color.parseColor("#DDDDDD"), PorterDuff.Mode.SRC_IN)

                                val position = r to c
                                if (horizontalRoads.contains(position)) {
                                    rotation = 90f
                                } else {
                                    rotation = 0f
                                }
                                
                                val horizontalPadding = (spotWidth * 0.3).toInt()
                                val verticalPadding = (spotHeight * 0.35).toInt()
                                setPadding(
                                    horizontalPadding,
                                    verticalPadding,
                                    horizontalPadding,
                                    verticalPadding
                                )
                            }
                            frameLayout.addView(lineImageView)
                        }
                        spotMap.containsKey(r to c) -> {
                            val spot = spotMap[r to c]!!
                            val isDisabled = spot.spotIdentifier?.contains("Disabled", ignoreCase = true) ?: false
                            
                            // Modern spot background with gradient and elevation
                            val border = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                
                                // Set colors based on spot status and type
                                when {
                                    isDisabled && spot.occupied -> {
                                        // Disabled + Occupied
                                        colors = intArrayOf(
                                            Color.parseColor("#6B8CFF"),
                                            Color.parseColor("#4F69BC")
                                        )
                                    }
                                    isDisabled -> {
                                        // Disabled + Available
                                        colors = intArrayOf(
                                            Color.parseColor("#8BA3FF"),
                                            Color.parseColor("#6B8CFF")
                                        )
                                    }
                                    spot.occupied -> {
                                        // Regular + Occupied
                                        colors = intArrayOf(
                                            Color.parseColor("#FF7F7F"),
                                            Color.parseColor("#FF5050")
                                        )
                                    }
                                    else -> {
                                        // Regular + Available
                                        colors = intArrayOf(
                                            Color.parseColor("#FFFFFF"),
                                            Color.parseColor("#F7F7F7")
                                        )
                                    }
                                }
                                
                                orientation = GradientDrawable.Orientation.TL_BR
                                cornerRadius = 12f
                                setStroke(3, Color.parseColor("#202020"))
                            }
                            
                            frameLayout.background = border
                            
                            // Add elevation effect
                            frameLayout.elevation = 5f
                                                
                            // Add spot identifier text
                            val textView = TextView(requireContext()).apply {
                                text = spot.spotIdentifier ?: spot.id.toString()
                                gravity = Gravity.CENTER
                                textSize = 14f
                                setTextColor(if (spot.occupied || isDisabled) Color.WHITE else Color.BLACK)
                                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                                    topMargin = dpToPx(8)
                                }
                            }
                            frameLayout.addView(textView)
                            
                            // Add car image if spot is occupied
                            if (spot.occupied) {
                                val occupiedIndicator = View(requireContext()).apply {
                                    background = GradientDrawable().apply {
                                        shape = GradientDrawable.RECTANGLE
                                        cornerRadius = 8f
                                        setColor(Color.parseColor("#FF5252"))
                                    }
                                    alpha = 0.2f
                                    layoutParams = FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT
                                    )
                                }
                                frameLayout.addView(occupiedIndicator)

                                val carImageView = ImageView(requireContext()).apply {
                                    setImageResource(R.drawable.top_car)
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                    rotation = 90f

                                    // Araç görselinin boyutlarını büyütün
                                    val width = (spotWidth * 0.75).toInt()   // Genişliği azalt
                                    val height = (spotHeight * 0.9).toInt()  // Yüksekliği artır

                                    layoutParams = FrameLayout.LayoutParams(
                                        width,
                                        height
                                    ).apply {
                                        gravity = Gravity.CENTER
                                        setMargins(0, 0, 0, 0) // Marjin olmadan
                                    }

                                    // Make sure the image is visible
                                    adjustViewBounds = true
                                    background = null
                                }
                                frameLayout.addView(carImageView)
                                
                                // Text'i yeniden öne getir
                                textView.bringToFront()
                            }
                            
                            // Add wheelchair icon for disabled spots
                            if (isDisabled) {
                                val wheelchairImageView = ImageView(requireContext()).apply {
                                    setImageResource(R.drawable.ic_wheelchair)
                                    scaleType = ImageView.ScaleType.FIT_XY
                                    setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                                    layoutParams = FrameLayout.LayoutParams(
                                        dpToPx(24),
                                        dpToPx(24)
                                    ).apply {
                                        gravity = Gravity.BOTTOM or Gravity.END
                                        setMargins(0, 0, dpToPx(8), dpToPx(8))
                                    }
                                }
                                frameLayout.addView(wheelchairImageView)
                            }
                            
                            // Store reference to the view for WebSocket updates
                            spotViews[spot.id.toLong()] = frameLayout
                            Log.d("ParkingLayout", "Added spot view to map - ID: ${spot.id}")
                        }
                        else -> {
                            // Empty cell styling
                            val emptyBackground = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                cornerRadius = 8f
                                setColor(Color.parseColor("#F9F9F9"))
                            }
                            frameLayout.background = emptyBackground
                        }
                    }

                    grid.addView(frameLayout, params)
                }
            }

            // İstatistikleri başlangıçta hesapla
            totalSpotCount = layout.parkingSpots.size
            occupiedSpotCount = layout.parkingSpots.count { it.occupied }
            
            // İstatistikleri güncelle
            updateParkingStats()
        } catch (e: Exception) {
            Log.e("ParkingLayout", "Error drawing layout: ${e.message}", e)
            Snackbar.make(
                binding.root,
                "Error displaying layout: ${e.message}",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun setupParkingStats(layout: ParkingLayoutResponse) {
        try {
            binding.tvParkingName.text = layout.parkingName

            val totalSpots = layout.parkingSpots.size
            val occupiedSpots = layout.parkingSpots.count { it.occupied }
            val availableSpots = totalSpots - occupiedSpots

            binding.tvAvailable.text = "Available: $availableSpots"
            binding.tvOccupied.text = "Occupied: $occupiedSpots"
            binding.tvLastUpdated.text = "Last updated: Just now"
        } catch (e: Exception) {
            Log.e("ParkingLayout", "Error setting up stats: ${e.message}", e)
        }
    }

    private fun updateParkingStats() {
        try {
            val availableSpots = totalSpotCount - occupiedSpotCount
            
            binding.tvAvailable.text = "Available: $availableSpots"
            binding.tvOccupied.text = "Occupied: $occupiedSpotCount"
            binding.tvLastUpdated.text = "Last updated: Just now"
            
            Log.d("ParkingLayout", "Updated stats - Available: $availableSpots, Occupied: $occupiedSpotCount")
        } catch (e: Exception) {
            Log.e("ParkingLayout", "Error updating stats: ${e.message}", e)
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
}