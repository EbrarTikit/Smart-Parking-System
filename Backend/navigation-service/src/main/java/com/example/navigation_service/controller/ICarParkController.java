package com.example.navigation_service.controller;

import java.util.List;

import com.example.navigation_service.dto.DtoCarPark;

public interface ICarParkController {

    public DtoCarPark getParkLocation(Long id);

    public List<DtoCarPark> getAllParkLocation();

}
