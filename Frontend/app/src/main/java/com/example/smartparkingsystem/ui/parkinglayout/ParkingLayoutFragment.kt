package com.example.smartparkingsystem.ui.parkinglayout

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
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
import com.example.smartparkingsystem.data.model.Spot
import com.example.smartparkingsystem.data.model.Road
import com.example.smartparkingsystem.data.remote.ParkingWebSocketClient
import com.example.smartparkingsystem.databinding.FragmentParkingLayoutBinding
import com.example.smartparkingsystem.utils.state.UiState
import android.view.animation.AccelerateDecelerateInterpolator
import android.animation.ValueAnimator
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ParkingLayoutFragment : Fragment() {

    companion object {
        private const val TAG = "ParkingLayout"
        private const val CAR_WIDTH_RATIO = 1.6
        private const val CAR_HEIGHT_RATIO = 3.2
        private const val HEIGHT_TO_WIDTH_RATIO = 2.0
        private const val MIN_SCALE = 0.5f
        private const val MAX_SCALE = 2.0f
        private const val ZOOM_SPEED = 1.4f
        private const val INVALID_POINTER_ID = -1

        // UI colors
        private const val COLOR_WHITE = "#FFFFFF"
        private const val COLOR_LIGHT_GRAY = "#F7F7F7"
        private const val COLOR_VERY_LIGHT_GRAY = "#F9F9F9"
        private const val COLOR_MEDIUM_GRAY = "#DDDDDD"
        private const val COLOR_DARK_GRAY = "#202020"

        // Occupied spots
        private const val COLOR_RED_LIGHT = "#FF7F7F"
        private const val COLOR_RED_DARK = "#FF5050"
        private const val COLOR_RED_ACCENT = "#FF5252"

        // Disabled spots
        private const val COLOR_BLUE_LIGHTER = "#8BA3FF"
        private const val COLOR_BLUE_LIGHT = "#6B8CFF"
        private const val COLOR_BLUE_MEDIUM = "#4F69BC"
        private const val COLOR_BLUE_OCCUPIED_LIGHT = "#2196F3"
        private const val COLOR_BLUE_OCCUPIED_DARK = "#1976D2"

        // Dimensions
        private const val SPOT_BASE_WIDTH_DP = 70
        private const val CORNER_RADIUS = 12f
    }

    private var _binding: FragmentParkingLayoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ParkingLayoutViewModel by viewModels()
    private var webSocketClient: ParkingWebSocketClient? = null
    private var scaleFactor = 1.0f
    private var focusX = 0f
    private var focusY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = INVALID_POINTER_ID
    private var lastDistance = 0f
    private var isScaling = false
    private var initialScale = 1.0f
    private var lastScaleFactor = 1.0f
    private var lastFocusX = 0f
    private var lastFocusY = 0f
    private var originalGridWidth = 0
    private var originalGridHeight = 0

    // Maps to track UI state
    private val spotViews = mutableMapOf<Long, FrameLayout>()
    private var totalSpotCount = 0
    private var occupiedSpotCount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView started")
        try {
            _binding = FragmentParkingLayoutBinding.inflate(inflater, container, false)
            validateBinding()
            return binding.root
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreateView: ${e.message}", e)
            return createFallbackView(e.message)
        }
    }

    private fun validateBinding() {
        if (binding.gridLayout == null) {
            Log.e(TAG, "gridLayout is null in binding")
        }
        if (binding.btnBack == null) {
            Log.e(TAG, "btnBack is null in binding")
        }
        if (binding.tvParkingName == null) {
            Log.e(TAG, "tvParkingName is null in binding")
        }
    }

    private fun createFallbackView(errorMessage: String?): View {
        return TextView(requireContext()).apply {
            text = "Failed to load parking layout: $errorMessage"
            gravity = Gravity.CENTER
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupTouchHandling()
        val parkingId = arguments?.getInt("parkingId") ?: 0
        observeParkingLayout(parkingId)
        initializeWebSocket()
    }

    private fun setupToolbar() {
        try {
            binding.btnBack.setOnClickListener {
                findNavController().navigateUp()
            } ?: Log.e(TAG, "Back button not found")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up toolbar: ${e.message}", e)
        }
    }

    private fun setupTouchHandling() {
        val container = binding.containerLayout
        container.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    activePointerId = event.getPointerId(0)
                    true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount == 2) {
                        isScaling = true
                        val index1 = event.findPointerIndex(activePointerId)
                        val index2 = event.findPointerIndex(event.getPointerId(1))
                        
                        if (index1 != -1 && index2 != -1) {
                            val x1 = event.getX(index1)
                            val y1 = event.getY(index1)
                            val x2 = event.getX(index2)
                            val y2 = event.getY(index2)
                            
                            lastDistance = calculateDistance(x1, y1, x2, y2)
                            initialScale = scaleFactor
                            lastScaleFactor = scaleFactor
                            
                            // Store the focus point
                            lastFocusX = (x1 + x2) / 2
                            lastFocusY = (y1 + y2) / 2
                            lastTouchX = lastFocusX
                            lastTouchY = lastFocusY
                        }
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isScaling && event.pointerCount == 2) {
                        val index1 = event.findPointerIndex(activePointerId)
                        val index2 = event.findPointerIndex(event.getPointerId(1))
                        
                        if (index1 != -1 && index2 != -1) {
                            val x1 = event.getX(index1)
                            val y1 = event.getY(index1)
                            val x2 = event.getX(index2)
                            val y2 = event.getY(index2)
                            
                            val currentDistance = calculateDistance(x1, y1, x2, y2)
                            val distanceChange = currentDistance - lastDistance
                            
                            // Calculate new scale factor
                            val scaleChange = (distanceChange / lastDistance) * ZOOM_SPEED
                            val newScale = scaleFactor * (1 + scaleChange)
                            
                            // Apply scale with bounds
                            scaleFactor = newScale.coerceIn(MIN_SCALE, MAX_SCALE)
                            
                            // Apply scale immediately
                            binding.gridLayout.scaleX = scaleFactor
                            binding.gridLayout.scaleY = scaleFactor
                            
                            // Adjust GridLayout margins based on scale factor
                            updateGridLayoutMargins(scaleFactor)
                            
                            // Calculate the new focus point
                            val currentFocusX = (x1 + x2) / 2
                            val currentFocusY = (y1 + y2) / 2
                            
                            // Calculate the translation needed to keep the focus point stable
                            val dx = (currentFocusX - lastFocusX) * scaleFactor
                            val dy = (currentFocusY - lastFocusY) * scaleFactor
                            
                            // Adjust scroll position to maintain focus point
                            binding.horizontalScrollView.scrollBy(-dx.toInt(), 0)
                            binding.verticalScrollView.scrollBy(0, -dy.toInt())
                            
                            lastDistance = currentDistance
                            lastFocusX = currentFocusX
                            lastFocusY = currentFocusY
                            lastTouchX = currentFocusX
                            lastTouchY = currentFocusY
                        }
                    } else if (!isScaling) {
                        // Single pointer move - handle scrolling
                        val index = event.findPointerIndex(activePointerId)
                        if (index != -1) {
                            val x = event.getX(index)
                            val y = event.getY(index)
                            
                            val dx = x - lastTouchX
                            val dy = y - lastTouchY
                            
                            // Adjust scroll speed based on zoom level
                            val scrollSpeed = 1.0f / scaleFactor
                            binding.horizontalScrollView.scrollBy((-dx * scrollSpeed).toInt(), 0)
                            binding.verticalScrollView.scrollBy(0, (-dy * scrollSpeed).toInt())
                            
                            lastTouchX = x
                            lastTouchY = y
                        }
                    }
                    true
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    if (event.pointerCount == 2) {
                        isScaling = false
                        val index = event.actionIndex
                        val pointerId = event.getPointerId(index)
                        if (pointerId == activePointerId) {
                            val newIndex = if (index == 0) 1 else 0
                            lastTouchX = event.getX(newIndex)
                            lastTouchY = event.getY(newIndex)
                            activePointerId = event.getPointerId(newIndex)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    isScaling = false
                    activePointerId = INVALID_POINTER_ID
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    isScaling = false
                    activePointerId = INVALID_POINTER_ID
                    true
                }
                else -> false
            }
        }
    }

    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private fun observeParkingLayout(parkingId: Int) {
        try {
            Log.d(TAG, "Loading layout for parking ID: $parkingId")
            viewModel.getParkingLayout(parkingId)
            viewModel.layout.observe(viewLifecycleOwner) { state ->
                when (state) {
                    is UiState.Loading -> {
                        Log.d(TAG, "Loading parking layout")
                        showLoading(true)
                    }
                    is UiState.Success -> {
                        Log.d(TAG, "Layout loaded successfully: ${state.data.parkingName}")
                        showLoading(false)
                        drawLayout(state.data)
                    }
                    is UiState.Error -> {
                        Log.e(TAG, "Error loading layout: ${state.message}")
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error observing layout: ${e.message}", e)
        }
    }

    private fun showError(message: String?) {
        Snackbar.make(
            binding.root,
            message ?: "Error loading parking layout",
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun initializeWebSocket() {
        Log.d(TAG, "Initializing WebSocket connection...")
        webSocketClient = ParkingWebSocketClient { update ->
            Log.d(TAG, "Received WebSocket update - ID: ${update.id}, Occupied: ${update.occupied}")
            activity?.runOnUiThread {
                updateParkingSpotStatus(update)
            }
        }

        webSocketClient?.setConnectionLostTimeout(0)
        
        try {
            webSocketClient?.connect()
            Log.d(TAG, "WebSocket connection initiated")
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to WebSocket: ${e.message}")
        }
    }

    private fun updateParkingSpotStatus(update: SensorUpdateDto) {
        val frameLayout = getSpotFrameLayout(update.id) ?: return
        
        activity?.runOnUiThread {
            updateOccupancyStatistics(frameLayout, update.occupied)

            // Find the spot identifier text
            val textView = findSpotIdentifier(frameLayout)
            val isDisabled = textView?.text?.contains("Disabled", ignoreCase = true) ?: false

            // Apply styling based on spot status
            applyParkingSpotBackground(frameLayout, isDisabled, update.occupied)
            updateTextColor(textView, isDisabled, update.occupied)

            // Redraw the spot content
            redrawSpotContent(frameLayout, textView, isDisabled, update.occupied)

            Log.d(
                TAG,
                "Updated spot ID: ${update.id} to ${if (update.occupied) "occupied" else "available"}"
            )
        }
    }

    private fun getSpotFrameLayout(spotId: Long): FrameLayout? {
        val view = spotViews[spotId]
        if (view == null) {
            Log.e(TAG, "No view found for spot ID: $spotId")
            return null
        }

        Log.d(TAG, "Found view for spot ID: $spotId")
        return view
    }

    private fun findSpotIdentifier(frameLayout: FrameLayout): TextView? {
        return frameLayout.findViewWithTag<TextView>("spotIdentifier")
            ?: frameLayout.getChildAt(0) as? TextView
    }

    private fun updateOccupancyStatistics(frameLayout: FrameLayout, isOccupied: Boolean) {
        val wasOccupied = frameLayout.findViewWithTag<View>("car_image") != null

        if (wasOccupied != isOccupied) {
            if (isOccupied) {
                occupiedSpotCount++
            } else {
                occupiedSpotCount--
            }
            updateParkingStats()
        }
    }

    private fun redrawSpotContent(
        frameLayout: FrameLayout,
        textView: TextView?,
        isDisabled: Boolean,
        isOccupied: Boolean,
    ) {
        // Remove all views but keep a reference to the identifier text
        frameLayout.removeAllViews()

        // Add back the identifier text
        if (textView != null) {
            frameLayout.addView(textView)
        }

        // Add car image if spot is occupied
        if (isOccupied) {
            addOccupiedIndicator(frameLayout)
            addCarImage(frameLayout)

            // Bring text to front
            textView?.bringToFront()
        }

        // Add wheelchair icon for disabled spots
        if (isDisabled) {
            addWheelchairIcon(frameLayout)
        }

    }

    override fun onDestroyView() {
        Log.d(TAG, "Destroying view and closing WebSocket")
        super.onDestroyView()
        webSocketClient?.close()
        webSocketClient = null
        spotViews.clear()
        _binding = null
        Log.d(TAG, "View destroyed and WebSocket closed")
    }

    private fun drawLayout(layout: ParkingLayoutResponse) {
        try {
            Log.d(
                TAG,
                "Drawing layout: ${layout.parkingName}, rows: ${layout.rows}, cols: ${layout.columns}"
            )

            setupParkingStats(layout)
            prepareGridForDrawing(layout)

            val spotWidth = dpToPx(SPOT_BASE_WIDTH_DP)
            val spotHeight = (spotWidth * HEIGHT_TO_WIDTH_RATIO).toInt()

            val spotMap = layout.parkingSpots.associateBy { it.row to it.column }
            val roadMap = layout.roads.associateBy { it.roadRow to it.roadColumn }
            val roadSet = roadMap.keys
            val buildingSet = layout.buildings.map { it.buildingRow to it.buildingColumn }.toSet()
            val roadOrientations = determineRoadOrientations(layout.roads, roadSet)

            drawGridCells(
                layout.rows,
                layout.columns,
                spotWidth,
                spotHeight,
                spotMap,
                roadMap,
                buildingSet,
                roadOrientations
            )

            // Store original grid dimensions after drawing and set initial margins
            binding.gridLayout.post { // Wait for layout pass to get dimensions
                originalGridWidth = binding.gridLayout.width
                originalGridHeight = binding.gridLayout.height
                updateGridLayoutMargins(scaleFactor)
            }

            // Calculate and update statistics
            totalSpotCount = layout.parkingSpots.size
            occupiedSpotCount = layout.parkingSpots.count { it.occupied }
            updateParkingStats()

        } catch (e: Exception) {
            Log.e(TAG, "Error drawing layout: ${e.message}", e)
            showError("Error displaying layout: ${e.message}")
        }
    }

    private fun prepareGridForDrawing(layout: ParkingLayoutResponse) {
        binding.gridLayout.apply {
            columnCount = layout.columns
            rowCount = layout.rows
            removeAllViews()
        }
        spotViews.clear()
    }

    private data class RoadOrientations(
        val horizontalRoads: Set<Pair<Int, Int>>,
        val verticalRoads: Set<Pair<Int, Int>>
    )

    private fun determineRoadOrientations(
        roads: List<Road>,
        roadSet: Set<Pair<Int, Int>>
    ): RoadOrientations {
        val horizontalRoads = mutableSetOf<Pair<Int, Int>>()
        val verticalRoads = mutableSetOf<Pair<Int, Int>>()

        for (road in roads) {
            val pos = road.roadRow to road.roadColumn
            val leftPos = road.roadRow to (road.roadColumn - 1)
            val rightPos = road.roadRow to (road.roadColumn + 1)
            val topPos = (road.roadRow - 1) to road.roadColumn
            val bottomPos = (road.roadRow + 1) to road.roadColumn

            if (roadSet.contains(leftPos) || roadSet.contains(rightPos)) {
                horizontalRoads.add(pos)
            } else if (roadSet.contains(topPos) || roadSet.contains(bottomPos)) {
                verticalRoads.add(pos)
            } else {
                // Default to vertical if no adjacent roads
                verticalRoads.add(pos)
            }
        }

        return RoadOrientations(horizontalRoads, verticalRoads)
    }

    private fun drawGridCells(
        rows: Int,
        columns: Int,
        spotWidth: Int,
        spotHeight: Int,
        spotMap: Map<Pair<Int, Int>, Spot>,
        roadMap: Map<Pair<Int, Int>, Road>,
        buildingSet: Set<Pair<Int, Int>>,
        roadOrientations: RoadOrientations
    ) {
        for (r in 0 until rows) {
            for (c in 0 until columns) {
                val frameLayout = FrameLayout(requireContext())
                val position = r to c

                val params = createGridLayoutParams(r, c, spotWidth, spotHeight)

                when {
                    roadMap.containsKey(position) -> {
                        val road = roadMap[position]
                        drawRoadCell(
                            frameLayout,
                            position,
                            road,
                            roadOrientations,
                            spotWidth,
                            spotHeight
                        )
                    }
                    spotMap.containsKey(position) -> {
                        val spot = spotMap[position]!!
                        drawParkingSpotCell(
                            frameLayout,
                            spot,
                            spotWidth,
                            spotHeight,
                            r,
                            c
                        )
                        // Store reference for WebSocket updates
                        spotViews[spot.id.toLong()] = frameLayout
                    }
                    buildingSet.contains(position) -> {
                        drawBuildingCell(
                            frameLayout
                        )
                    }
                    else -> {
                        drawEmptyCell(frameLayout)
                    }
                }

                binding.gridLayout.addView(frameLayout, params)
            }
        }
    }

    private fun createGridLayoutParams(
        row: Int,
        col: Int,
        width: Int,
        height: Int
    ): GridLayout.LayoutParams {
        return GridLayout.LayoutParams(
            GridLayout.spec(row, 1, GridLayout.CENTER),
            GridLayout.spec(col, 1, GridLayout.CENTER)
        ).apply {
            this.width = width
            this.height = height
            setMargins(8, 8, 8, 8)
        }
    }

    private fun drawRoadCell(
        frameLayout: FrameLayout,
        position: Pair<Int, Int>,
        road: Road?,
        roadOrientations: RoadOrientations,
        spotWidth: Int,
        spotHeight: Int
    ) {
        // Set road background
        frameLayout.background = createRoadBackground()

        // Check the road identifier
        when (road?.roadIdentifier?.lowercase()) {
            "entry" -> {
                // Add entry text
                addRoadText(frameLayout, "ENTRY", Color.GREEN)
            }

            "exit" -> {
                // Add exit text
                addRoadText(frameLayout, "EXIT", Color.RED)
            }

            else -> {
                // Add regular road line
                val isHorizontal = roadOrientations.horizontalRoads.contains(position)
                addRoadLine(frameLayout, isHorizontal, spotWidth, spotHeight)
            }
        }
    }

    private fun createRoadBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 4f
            setColor(Color.parseColor(COLOR_WHITE))
        }
    }

    private fun addRoadLine(
        frameLayout: FrameLayout,
        isHorizontal: Boolean,
        spotWidth: Int,
        spotHeight: Int
    ) {
        val lineImageView = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_line)
            scaleType = ImageView.ScaleType.FIT_XY
            setColorFilter(Color.parseColor(COLOR_MEDIUM_GRAY), PorterDuff.Mode.SRC_IN)
            rotation = if (isHorizontal) 90f else 0f

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

    private fun drawParkingSpotCell(
        frameLayout: FrameLayout,
        spot: Spot,
        spotWidth: Int,
        spotHeight: Int,
        row: Int,
        col: Int
    ) {
        val isDisabled = spot.spotIdentifier?.contains("Disabled", ignoreCase = true) ?: false

        // Apply background styling
        applyParkingSpotBackground(frameLayout, isDisabled, spot.occupied)

        // Add elevation effect
        frameLayout.elevation = 5f

        // Add spot identifier text
        val identifierText = getSpotIdentifier(spot, row, col)
        val textView = createSpotIdentifierText(identifierText, isDisabled, spot.occupied)
        frameLayout.addView(textView)

        // Add car image if spot is occupied
        if (spot.occupied) {
            addOccupiedIndicator(frameLayout)
            addCarImage(frameLayout, spotWidth, spotHeight)
            textView.bringToFront()
        }

        // Add wheelchair icon for disabled spots
        if (isDisabled) {
            addWheelchairIcon(frameLayout)
        }

    }

    private fun getSpotIdentifier(
        spot: Spot,
        row: Int,
        col: Int
    ): String {
        return if (spot.spotIdentifier.isNullOrEmpty()) {
            val rowLetter = ('A' + row).toChar()
            "$rowLetter${col + 1}"
        } else {
            spot.spotIdentifier
        }
    }

    private fun createSpotIdentifierText(
        identifier: String,
        isDisabled: Boolean,
        isOccupied: Boolean
    ): TextView {
        return TextView(requireContext()).apply {
            text = identifier
            gravity = Gravity.CENTER
            textSize = 8f
            setTextColor(if (isOccupied || isDisabled) Color.WHITE else Color.BLACK)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = dpToPx(5)
                bottomMargin = dpToPx(3)
            }
            tag = "spotIdentifier"
        }
    }

    private fun addOccupiedIndicator(frameLayout: FrameLayout) {
        val occupiedIndicator = View(requireContext()).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f
                setColor(Color.parseColor(COLOR_RED_ACCENT))
            }
            alpha = 0.2f
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        frameLayout.addView(occupiedIndicator)
    }

    private fun addCarImage(
        frameLayout: FrameLayout,
        spotWidth: Int? = null,
        spotHeight: Int? = null
    ) {
        val width: Int
        val height: Int

        if (spotWidth != null && spotHeight != null) {
            width = (spotWidth * CAR_WIDTH_RATIO).toInt()
            height = (spotHeight * CAR_HEIGHT_RATIO).toInt()
        } else {
            // For WebSocket updates, use the frameLayout dimensions
            width = (frameLayout.width * CAR_WIDTH_RATIO).toInt()
            height = (frameLayout.height * CAR_HEIGHT_RATIO).toInt()
        }

        val carImageView = ImageView(requireContext()).apply {
            setImageResource(R.drawable.top_car)
            scaleType = ImageView.ScaleType.FIT_XY
            rotation = 90f

            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                this.width = width
                this.height = height
                setMargins(0, 12, 0, 0)
            }

            adjustViewBounds = true
            background = null
            tag = "car_image"
        }
        frameLayout.addView(carImageView)
    }

    private fun addWheelchairIcon(frameLayout: FrameLayout) {
        val wheelchairImageView = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_wheelchair)
            scaleType = ImageView.ScaleType.FIT_XY
            setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(20),
                dpToPx(20)
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, dpToPx(4), dpToPx(4))
            }
        }
        frameLayout.addView(wheelchairImageView)
    }

    private fun drawEmptyCell(frameLayout: FrameLayout) {
        val emptyBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8f
            setColor(Color.parseColor(COLOR_VERY_LIGHT_GRAY))
        }
        frameLayout.background = emptyBackground
    }

    private fun applyParkingSpotBackground(
        frameLayout: FrameLayout,
        isDisabled: Boolean,
        isOccupied: Boolean
    ) {
        val border = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE

            when {
                isDisabled && isOccupied -> {
                    // Disabled + Occupied
                    colors = intArrayOf(
                        Color.parseColor(COLOR_BLUE_OCCUPIED_LIGHT),
                        Color.parseColor(COLOR_BLUE_OCCUPIED_DARK)
                    )
                }
                isDisabled -> {
                    // Disabled + Available
                    colors = intArrayOf(
                        Color.parseColor(COLOR_BLUE_LIGHTER),
                        Color.parseColor(COLOR_BLUE_LIGHT)
                    )
                }

                isOccupied -> {
                    // Regular + Occupied
                    colors = intArrayOf(
                        Color.parseColor(COLOR_RED_LIGHT),
                        Color.parseColor(COLOR_RED_DARK)
                    )
                }

                else -> {
                    // Regular + Available
                    colors = intArrayOf(
                        Color.parseColor(COLOR_WHITE),
                        Color.parseColor(COLOR_LIGHT_GRAY)
                    )
                }
            }

            orientation = GradientDrawable.Orientation.TL_BR
            cornerRadius = CORNER_RADIUS
            setStroke(3, Color.parseColor(COLOR_DARK_GRAY))
        }

        frameLayout.background = border
    }

    private fun updateTextColor(
        textView: TextView?,
        isDisabled: Boolean,
        isOccupied: Boolean
    ) {
        textView?.setTextColor(
            if (isOccupied || isDisabled) Color.WHITE else Color.BLACK
        )
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
            Log.e(TAG, "Error setting up stats: ${e.message}", e)
        }
    }

    private fun updateParkingStats() {
        try {
            val availableSpots = totalSpotCount - occupiedSpotCount
            
            binding.tvAvailable.text = "Available: $availableSpots"
            binding.tvOccupied.text = "Occupied: $occupiedSpotCount"
            binding.tvLastUpdated.text = "Last updated: Just now"

            Log.d(TAG, "Updated stats - Available: $availableSpots, Occupied: $occupiedSpotCount")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating stats: ${e.message}", e)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.apply {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun addRoadText(frameLayout: FrameLayout, text: String, textColor: Int) {
        val textView = TextView(requireContext()).apply {
            this.text = text
            this.setTextColor(textColor)
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }
        frameLayout.addView(textView)
    }

    private fun drawBuildingCell(frameLayout: FrameLayout) {
        // Set building background
        val buildingBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#FFFFFF"))  // Light gray background
            cornerRadius = 4f
        }
        frameLayout.background = buildingBackground

        // Add building indicator (black square)
        addBuildingIndicator(frameLayout)
    }

    private fun addBuildingIndicator(frameLayout: FrameLayout) {
        val blackSquare = View(requireContext()).apply {
            // Siyah arka plan ayarla
            setBackgroundColor(Color.BLACK)

            // 40x40dp boyutunda görünüm oluştur
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(40),
                dpToPx(40)
            ).apply {
                gravity = Gravity.CENTER // Merkeze konumlandır
            }
        }

        // Kareyi frameLayout'a ekle
        frameLayout.addView(blackSquare)
    }

    private fun updateGridLayoutMargins(scale: Float) {
        if (originalGridWidth == 0 || originalGridHeight == 0) {
            // Dimensions not yet measured, wait for post
            return
        }

        val scaledWidth = originalGridWidth * scale
        val scaledHeight = originalGridHeight * scale

        // Calculate required margins to allow scrolling to the scaled edges
        // These margins represent the extra space needed beyond the original grid size
        val marginHorizontal = ((scaledWidth - originalGridWidth) / 2).toInt()
        val marginVertical = ((scaledHeight - originalGridHeight) / 2).toInt()

        val layoutParams = binding.gridLayout.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical)
        binding.gridLayout.layoutParams = layoutParams
    }
}