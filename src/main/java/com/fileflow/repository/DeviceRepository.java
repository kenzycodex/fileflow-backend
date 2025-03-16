package com.fileflow.repository;

import com.fileflow.model.Device;
import com.fileflow.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    List<Device> findByUser(User user);

    List<Device> findByUserAndDeviceNameAndDeviceTypeAndPlatform(
            User user, String deviceName, String deviceType, String platform);

    List<Device> findByLastActiveAfter(LocalDateTime dateTime);

    int countByUserAndLastActiveAfter(User user, LocalDateTime dateTime);
}