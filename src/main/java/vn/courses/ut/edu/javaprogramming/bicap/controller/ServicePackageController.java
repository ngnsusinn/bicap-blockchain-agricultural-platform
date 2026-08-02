package vn.courses.ut.edu.javaprogramming.bicap.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ServicePackageResponse;
import vn.courses.ut.edu.javaprogramming.bicap.service.ServicePackageService;

import java.util.List;

@RestController
@RequestMapping("/api/service-packages")
public class ServicePackageController {

    private final ServicePackageService servicePackageService;

    public ServicePackageController(ServicePackageService servicePackageService) {
        this.servicePackageService = servicePackageService;
    }

    @GetMapping
    public ResponseEntity<List<ServicePackageResponse>> getAllActivePackages() {
        return ResponseEntity.ok(servicePackageService.getAllActivePackages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicePackageResponse> getPackageById(@PathVariable Long id) {
        return ResponseEntity.ok(servicePackageService.getPackageById(id));
    }
}
