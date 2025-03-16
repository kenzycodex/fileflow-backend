package com.fileflow.service.device;

import com.fileflow.dto.response.common.ApiResponse;
import com.fileflow.dto.response.common.PagedResponse;
import com.fileflow.model.Device;

public interface DeviceService {
    /**
     * Get user devices
     *
     * @param page page number
     * @param size page size
     * @return paged list of devices
     */
    PagedResponse<Device> getUserDevices(int page, int size);

    /**
     * Register new device
     *
     * @param deviceName device name
     * @param deviceType device type (android, ios, desktop)
     * @param platform platform information
     * @param pushToken optional push token
     * @return the registered device
     */
    Device registerDevice(String deviceName, String deviceType, String platform, String pushToken);

    /**
     * Update device
     *
     * @param deviceId device ID
     * @param deviceName new device name
     * @param pushToken new push token
     * @return the updated device
     */
    Device updateDevice(Long deviceId, String deviceName, String pushToken);

    /**
     * Delete device
     *
     * @param deviceId device ID
     * @return response with deletion information
     */
    ApiResponse deleteDevice(Long deviceId);

    /**
     * Sync device
     *
     * @param deviceId device ID
     * @return response with sync information
     */
    ApiResponse syncDevice(Long deviceId);

    /**
     * Get device details
     *
     * @param deviceId device ID
     * @return the device
     */
    Device getDevice(Long deviceId);
}