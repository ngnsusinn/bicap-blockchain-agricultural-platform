package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Partial update of a deployed/managed smart contract (F6 — SRS-ADM-005).
 *
 * <p>All fields are optional: only the supplied ones are applied. {@code status} must be
 * one of PENDING, DEPLOYED, ACTIVE, INACTIVE, FAILED.
 */
public class UpdateContractRequest {

    @Size(max = 100)
    private String name;

    private String bytecode;

    private String abi;

    @Pattern(regexp = "(?i)TESTNET|MAINNET", message = "environment must be TESTNET or MAINNET")
    private String environment;

    @Size(max = 20)
    private String version;

    @Pattern(regexp = "(?i)PENDING|DEPLOYED|ACTIVE|INACTIVE|FAILED",
            message = "status must be PENDING, DEPLOYED, ACTIVE, INACTIVE or FAILED")
    private String status;

    @Pattern(regexp = "^$|^0x[0-9a-fA-F]{40}$", message = "address must be a 0x-prefixed 20-byte hex address")
    private String address;

    public UpdateContractRequest() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBytecode() { return bytecode; }
    public void setBytecode(String bytecode) { this.bytecode = bytecode; }
    public String getAbi() { return abi; }
    public void setAbi(String abi) { this.abi = abi; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
