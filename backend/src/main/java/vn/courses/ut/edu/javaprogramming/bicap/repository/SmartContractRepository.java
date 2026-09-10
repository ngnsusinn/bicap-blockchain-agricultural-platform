package vn.courses.ut.edu.javaprogramming.bicap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SmartContract;

import java.util.List;
import java.util.Optional;

@Repository
public interface SmartContractRepository extends JpaRepository<SmartContract, Long> {
    Optional<SmartContract> findByName(String name);
    Optional<SmartContract> findByAddress(String address);
    List<SmartContract> findByEnvironment(String environment);
    Optional<SmartContract> findByNameAndEnvironment(String name, String environment);
}
