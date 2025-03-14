package com.example.navigation_service.controller.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.navigation_service.controller.ICarParkController;
import com.example.navigation_service.dto.DtoCarPark;
import com.example.navigation_service.service.ICarParkService;

@RestController
@RequestMapping("/rest/api/car_park")
public class CarParkControllerImpl implements ICarParkController {

    @Autowired
    private ICarParkService carParkService;

    @GetMapping(path =  "/list/{id}")
    @Override
    public DtoCarPark getParkLocation(Long id) {
        return carParkService.getParkLocation(id);
    }

    @GetMapping(path =  "/list")
    @Override
    public List<DtoCarPark> getAllParkLocation() {
        return carParkService.getAllParkLocation();
    }
}
