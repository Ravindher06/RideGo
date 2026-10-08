package com.rapido.rider.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicles", schema = "rider_schema")
public class Vehicle {

    @Id
    private Long vehicleId;

    @Column(nullable = false, unique = true, length = 20)
    private String vehicleNumber;

    @Column(nullable = false, length = 20)
    private String vehicleType;

    @Column(nullable = false, length = 50)
    private String vehicleBrand;

    @Column(nullable = false, length = 50)
    private String vehicleModel;

    @Column(nullable = false, length = 30)
    private String vehicleColor;

    @Column(nullable = false)
    private Integer vehicleCapacity;

    @Column(nullable = false, unique = true, length = 50)
    private String registrationNumber;

    @Column(nullable = false, unique = true, length = 50)
    private String insuranceNumber;

    @Column(nullable = false, unique = true, length = 50)
    private String pollutionCertificate;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "vehicle_id")
    private Rider rider;

    // Getters and Setters
    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

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

    public Rider getRider() {
        return rider;
    }

    public void setRider(Rider rider) {
        this.rider = rider;
    }
}
