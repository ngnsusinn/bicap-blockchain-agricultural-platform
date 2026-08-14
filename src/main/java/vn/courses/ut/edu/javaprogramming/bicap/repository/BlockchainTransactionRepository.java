package vn.courses.ut.edu.javaprogramming.bicap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.courses.ut.edu.javaprogramming.bicap.entity.BlockchainTransaction;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockchainTransactionRepository extends JpaRepository<BlockchainTransaction, Long> {
    Optional<BlockchainTransaction> findByTxHash(String txHash);
    Optional<BlockchainTransaction> findByIdempotencyKey(String idempotencyKey);
    List<BlockchainTransaction> findByStatus(String status);
    List<BlockchainTransaction> findByEntityTypeAndEntityId(String entityType, Long entityId);
}
