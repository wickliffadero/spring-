package com.example.frauddetction.Repository;

import com.example.frauddetction.model.DeviceFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceFingerprintRepository extends JpaRepository<DeviceFingerprint, Long> {
    DeviceFingerprint findByDeviceIdAndUsername(String deviceId, String username);
    boolean existsByDeviceIdAndUsername(String deviceId, String username);
} 