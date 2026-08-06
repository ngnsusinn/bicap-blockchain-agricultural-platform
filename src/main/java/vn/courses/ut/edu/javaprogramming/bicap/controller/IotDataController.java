package vn.courses.ut.edu.javaprogramming.bicap.controller;

import vn.courses.ut.edu.javaprogramming.bicap.dto.IotDataRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.IotData;
import vn.courses.ut.edu.javaprogramming.bicap.service.IotDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/iot/sensors")
@CrossOrigin(origins = "*") // Allows standalone frontend to connect easily
public class IotDataController {

    @Autowired
    private IotDataService iotDataService;

    @PostMapping
    public ResponseEntity<IotData> receiveSensorData(@RequestBody IotDataRequest request) {
        IotData savedData = iotDataService.saveAndCheckThresholds(request);
        return ResponseEntity.ok(savedData);
    }
}
