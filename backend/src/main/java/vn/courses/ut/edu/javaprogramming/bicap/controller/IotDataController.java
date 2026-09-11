package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import vn.courses.ut.edu.javaprogramming.bicap.dto.IotDataRequest;
import vn.courses.ut.edu.javaprogramming.bicap.entity.IotData;
import vn.courses.ut.edu.javaprogramming.bicap.service.IotDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * IoT sensor ingestion (BICAP-14).
 *
 * <p>C4 fix: the endpoint is no longer open to every authenticated role. The acting user
 * must be the owner of the target farm (or a platform admin) and the payload is validated
 * — previously any retailer/driver token could write arbitrary temperature/humidity/pH
 * values into any farm, poisoning the traceability data shown to consumers.
 */
@RestController
@RequestMapping("/api/iot/sensors")
public class IotDataController {

    private final IotDataService iotDataService;

    public IotDataController(IotDataService iotDataService) {
        this.iotDataService = iotDataService;
    }

    @PostMapping
    public ResponseEntity<IotData> receiveSensorData(@Valid @RequestBody IotDataRequest request) {
        IotData savedData = iotDataService.saveAndCheckThresholds(request);
        return ResponseEntity.ok(savedData);
    }
}
