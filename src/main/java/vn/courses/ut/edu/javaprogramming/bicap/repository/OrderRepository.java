package vn.courses.ut.edu.javaprogramming.bicap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Order;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByRetailerId(Long retailerId);
    Optional<Order> findByDepositCode(String depositCode);
}