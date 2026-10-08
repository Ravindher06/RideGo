package com.rapido.rider.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class VehicleRequest {

    @NotBlank(message = "Vehicle number cannot be blank")
    private String vehicleNumber;

    @NotBlank(message = "Vehicle type cannot be blank")
    private String vehicleType; // BIKE, CAR, AUTO etc.

    @NotBlank(message = "Vehicle brand cannot be blank")
    private String vehicleBrand;

    @NotBlank(message = "Vehicle model cannot be blank")
    private String vehicleModel;

    @NotBlank(message = "Vehicle color cannot be blank")
    private String vehicleColor;

    @NotNull(message = "Vehicle capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer vehicleCapacity;

    @NotBlank(message = "Registration number cannot be blank")
    private String registrationNumber;

    @NotBlank(message = "Insurance number cannot be blank")
    private String insuranceNumber;

    @NotBlank(message = "Pollution certificate reference cannot be blank")
    private String pollutionCertificate;

    // Getters and Setters
    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getVehicleBrand() {
        return vehicleBrand;
    }

    public void setVehicleBrand(String vehicleBrand) {
        this.vehicleBrand = vehicleBrand;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getVehicleColor() {
        return vehicleColor;
    }

    public void setVehicleColor(String vehicleColor) {
        this.vehicleColor = vehicleColor;
    }

    public Integer getVehicleCapacity() {
        return vehicleCapacity;
    }

    public void setVehicleCapacity(Integer vehicleCapacity) {
        this.vehicleCapacity = vehicleCapacity;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getInsuranceNumber() {
        return insuranceNumber;
    }

    public void setInsuranceNumber(String insuranceNumber) {
        this.insuranceNumber = insuranceNumber;
    }

    public String getPollutionCertificate() {
        return pollutionCertificate;
    }

    public void setPollutionCertificate(String pollutionCertificate) {
        this.pollutionCertificate = pollutionCertificate;
    }
}
