package com.example.smartparkingsystem.ui.favorites

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartparkingsystem.R
import com.example.smartparkingsystem.data.model.FavoriteParking
import com.example.smartparkingsystem.data.model.ParkingListResponse
import com.example.smartparkingsystem.databinding.FragmentFavoritesBinding
import com.example.smartparkingsystem.utils.SessionManager
import com.example.smartparkingsystem.utils.state.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class FavoritesFragment : Fragment() {

    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FavoritesViewModel by viewModels()
    private lateinit var adapter: FavoriteParkingAdapter

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeFavorites()
        loadFavorites()
    }

    private fun setupRecyclerView() {
        adapter = FavoriteParkingAdapter(
            onItemClick = { favoriteParking ->
                navigateToDetail(favoriteParking)
            },
            onFavoriteClick = { favoriteParking ->
                removeFavorite(favoriteParking)
            }
        )

        binding.rvFavorites.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@FavoritesFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun observeFavorites() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.favoritesState.collectLatest { state ->
                when (state) {
                    is UiState.Loading -> showLoading()
                    is UiState.Success -> {
                        hideLoading()
                        if (state.data.isEmpty()) {
                            showEmptyState()
                        } else {
                            showFavorites(state.data)
                        }
                    }

                    is UiState.Error -> {
                        hideLoading()
                        Log.e("FavoritesFragment", state.message)
                        //showError(state.message)
                    }
                }
            }
        }
    }

    private fun loadFavorites() {
        val userId = sessionManager.getUserId()
        if (userId > 0) {
            viewModel.loadFavorites(userId.toInt())
        } else {
            showError("Please login to view favorites")
        }
    }

    private fun removeFavorite(favoriteParking: FavoriteParking) {
        val userId = sessionManager.getUserId()
        if (userId > 0) {
            viewModel.removeFavorite(userId.toInt(), favoriteParking.id)
            Log.d("FavoritesFragment", "Removed favorite: ${favoriteParking.name}")
            //Toast.makeText(requireContext(), "Removed from favorites", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToDetail(favoriteParking: FavoriteParking) {
        val parking = ParkingListResponse(
            id = favoriteParking.id,
            name = favoriteParking.name,
            location = favoriteParking.location,
            imageUrl = favoriteParking.imageUrl,
            rate = 10.0,
            openingHours = "08:00",
            closingHours = "22:00",
            latitude = 0.0,
            longitude = 0.0,
            capacity = 100,
            rows = 5,
            columns = 5,
            description = "",
            parkingSpots = emptyList()
        )

        findNavController().navigate(
            R.id.action_navigation_favorites_to_detailFragment,
            bundleOf("parking" to parking)
        )
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.rvFavorites.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showFavorites(favorites: List<FavoriteParking>) {
        binding.rvFavorites.visibility = View.VISIBLE
        binding.emptyStateContainer.visibility = View.GONE
        adapter.submitList(favorites)
    }

    private fun showEmptyState() {
        binding.rvFavorites.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        Log.e("FavoritesFragment", message)
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}