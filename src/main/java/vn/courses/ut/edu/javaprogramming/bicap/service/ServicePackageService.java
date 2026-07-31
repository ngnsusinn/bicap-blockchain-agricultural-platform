package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.courses.ut.edu.javaprogramming.bicap.dto.ServicePackageResponse;
import vn.courses.ut.edu.javaprogramming.bicap.entity.ServicePackage;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ServicePackageRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ServicePackageService {

    private final ServicePackageRepository servicePackageRepository;

    public ServicePackageService(ServicePackageRepository servicePackageRepository) {
        this.servicePackageRepository = servicePackageRepository;
    }

    public List<ServicePackageResponse> getAllActivePackages() {
        return servicePackageRepository.findAllByStatus("ACTIVE").stream()
                .map(ServicePackageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public ServicePackageResponse getPackageById(Long id) {
        ServicePackage servicePackage = servicePackageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service package not found"));
        return ServicePackageResponse.fromEntity(servicePackage);
    }
}
