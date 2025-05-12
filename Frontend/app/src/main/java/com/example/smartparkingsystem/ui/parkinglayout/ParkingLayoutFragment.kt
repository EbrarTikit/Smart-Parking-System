package com.example.smartparkingsystem.ui.parkinglayout

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
        webSocketClient = ParkingWebSocketClient { update ->
            activity?.runOnUiThread {
                updateParkingSpotStatus(update)
            }
        }
        webSocketClient?.connect()
    }

    private fun updateParkingSpotStatus(update: SensorUpdateDto) {
        spotViews[update.id]?.let { view ->
            val frameLayout = view as? FrameLayout ?: return
            val border = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(if (update.occupied) Color.RED else Color.GREEN)
                setStroke(3, Color.BLACK)
                cornerRadius = 12f
            }
            frameLayout.background = border
        }
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

                    //Creating border
                    val border = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(Color.WHITE)
                        setStroke(3, Color.BLACK)
                        cornerRadius = 12f

                        colors = intArrayOf(
                            Color.WHITE,
                            Color.parseColor("#F5F9FF")
                        )
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
                            val roadBackground = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                setColor(Color.parseColor("#FFFFFF"))
                                cornerRadius = 4f
                            }
                            frameLayout.background = roadBackground

                            val lineImageView = ImageView(requireContext()).apply {
                                setImageResource(R.drawable.ic_line)
                                scaleType = ImageView.ScaleType.FIT_XY
                                setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)

                                val position = r to c
                                //Horizontal road w rotation
                                if (horizontalRoads.contains(position)) {
                                    rotation = 90f

                                    val horizontalPadding = (spotWidth * 0.3).toInt()
                                    val verticalPadding = (spotHeight * 0.35).toInt()
                                    setPadding(
                                        horizontalPadding,
                                        verticalPadding,
                                        horizontalPadding,
                                        verticalPadding
                                    )
                                } else {
                                    // Vertical road - no rotation
                                    rotation = 0f

                                    val horizontalPadding = (spotWidth * 0.3).toInt()
                                    val verticalPadding = (spotHeight * 0.35).toInt()
                                    setPadding(
                                        horizontalPadding,
                                        verticalPadding,
                                        horizontalPadding,
                                        verticalPadding
                                    )
                                }
                            }
                            frameLayout.addView(lineImageView)
                        }
                        spotMap.containsKey(r to c) -> {
                            frameLayout.background = border

                            // ParkingSpot
                            val spot = spotMap[r to c]!!

                            // If PArkingSpot is null, create auto
                            val identifier = if (spot.spotIdentifier.isNullOrEmpty()) {
                                val rowLetter = ('A' + r).toChar()
                                "$rowLetter${c + 1}"
                            } else {
                                spot.spotIdentifier
                            }

                            //Disabled Parking Spot
                            // Check if this is a disabled parking spot
                            val isDisabledSpot = !spot.spotIdentifier.isNullOrEmpty() &&
                                    spot.spotIdentifier.contains("Disabled", ignoreCase = true)

                            if (isDisabledSpot) {
                                val disabledSpotBg = GradientDrawable().apply {
                                    shape = GradientDrawable.RECTANGLE
                                    setColor(Color.parseColor("#2196F3")) // Blue background
                                    setStroke(3, Color.parseColor("#1976D2"))  // Darker blue border
                                    cornerRadius = 12f
                                }
                                frameLayout.background = disabledSpotBg
                            }

                            val textView = TextView(requireContext()).apply {
                                text = identifier
                                gravity = Gravity.CENTER
                                setTextColor(if (isDisabledSpot) Color.WHITE else Color.BLACK)
                                textSize = 14f
                                layoutParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                    FrameLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                                    topMargin = dpToPx(4)
                                    setMargins(0, 2, 0, 2)
                                }
                            }
                            frameLayout.addView(textView)

                            // Availability check
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
                                    scaleType = ImageView.ScaleType.FIT_XY
                                    rotation = 90f

                                    val width = (spotWidth * 1.5).toInt()
                                    val height = (spotHeight * 1.7).toInt()

                                    layoutParams = FrameLayout.LayoutParams(
                                        width,
                                        height
                                    ).apply {
                                        gravity = Gravity.CENTER
                                        setMargins(0, 0, 0, 0)
                                    }

                                    // Make sure the image is visible
                                    adjustViewBounds = true
                                    background = null
                                }
                                frameLayout.addView(carImageView)
                            }

                            // Add wheelchair icon for disabled spots
                            if (isDisabledSpot) {
                                val wheelchairIcon = ImageView(requireContext()).apply {
                                    setImageResource(R.drawable.ic_wheelchair)
                                    layoutParams = FrameLayout.LayoutParams(
                                        dpToPx(24),
                                        dpToPx(24)
                                    ).apply {
                                        gravity = Gravity.BOTTOM or Gravity.END
                                        setMargins(0, 0, dpToPx(4), dpToPx(4))
                                    }
                                    setColorFilter(
                                        Color.parseColor("#1976D2"),
                                        PorterDuff.Mode.SRC_IN
                                    )
                                }
                                frameLayout.addView(wheelchairIcon)
                            }

                            // Store the view reference
                            spot.id?.let { id ->
                                spotViews[id.toLong()] = frameLayout
                            }
                        }
                        else -> {
                            val emptyBackground = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                cornerRadius = 8f
                                setColor(Color.parseColor("#FFFFFF"))
                            }
                            frameLayout.background = emptyBackground
                        }
                    }

                    grid.addView(frameLayout, params)
                }
            }
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
        webSocketClient?.close()
        webSocketClient = null
        spotViews.clear()
        _binding = null
    }
}