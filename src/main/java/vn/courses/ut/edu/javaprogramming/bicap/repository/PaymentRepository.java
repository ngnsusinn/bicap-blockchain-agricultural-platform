package vn.courses.ut.edu.javaprogramming.bicap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Payment;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTxRef(String txRef);
    List<Payment> findByOrderId(Long orderId);
}
