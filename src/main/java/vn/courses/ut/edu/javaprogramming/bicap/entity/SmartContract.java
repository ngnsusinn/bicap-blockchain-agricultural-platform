package vn.courses.ut.edu.javaprogramming.bicap.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Maps to the `smart_contracts` table.
 * Stores smart contract information deployed or pending deployment.
 */
@Entity
@Table(name = "smart_contracts")
public class SmartContract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 42)
    private String address;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String bytecode;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String abi;

    @Column(nullable = false, length = 20)
    private String environment = "TESTNET"; // TESTNET, MAINNET

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, DEPLOYED, ACTIVE, INACTIVE, FAILED

    @Column(nullable = false, length = 20)
    private String version = "1.0.0";

    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SmartContract() {
    }

    public SmartContract(Long id, String name, String address, String bytecode, String abi,
                         String environment, String status, String version, String txHash,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.bytecode = bytecode;
        this.abi = abi;
        this.environment = environment;
        this.status = status;
        this.version = version;
        this.txHash = txHash;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        if (this.status == null) {
            this.status = "PENDING";
        }
        if (this.environment == null) {
            this.environment = "TESTNET";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBytecode() {
        return bytecode;
    }

    public void setBytecode(String bytecode) {
        this.bytecode = bytecode;
    }

    public String getAbi() {
        return abi;
    }

    public void setAbi(String abi) {
        this.abi = abi;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
