package com.example.navigation_service.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.navigation_service.dto.DtoCarPark;
import com.example.navigation_service.model.CarPark;
import com.example.navigation_service.repository.ICarParkRepository;
import com.example.navigation_service.service.ICarParkService;

@Service
public class CarParkServiceImpl implements ICarParkService{

    @Autowired
    private ICarParkRepository carParkRepository;

    @Override
    public DtoCarPark getParkLocation(Long id) {
        DtoCarPark dtoCarPark = new DtoCarPark();
        Optional<CarPark> optional = carParkRepository.findById(id);
        if(optional.isPresent()) {
            CarPark carPark = optional.get();
            BeanUtils.copyProperties(carPark, dtoCarPark);
            return dtoCarPark;
        }
        return null;
    }

    @Override
    public List<DtoCarPark> getAllParkLocation() {
        List<DtoCarPark> dtoCarParks = new ArrayList<>();
        List<CarPark> carParks = carParkRepository.findAll();
        for (CarPark carPark : carParks) {
            DtoCarPark dtoCarPark = new DtoCarPark();
            BeanUtils.copyProperties(carPark, dtoCarPark);
            dtoCarParks.add(dtoCarPark);
        }
        return dtoCarParks;
    }

}
