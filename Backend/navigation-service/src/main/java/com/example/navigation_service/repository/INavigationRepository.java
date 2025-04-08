package com.example.navigation_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.navigation_service.model.CarPark;

@Repository
public interface INavigationRepository extends JpaRepository<CarPark,Long> {

}
