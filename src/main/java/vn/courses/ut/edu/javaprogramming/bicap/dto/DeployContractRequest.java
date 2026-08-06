package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.NotBlank;

public class DeployContractRequest {

    @NotBlank(message = "Contract name is required")
    private String name;

    @NotBlank(message = "Bytecode is required")
    private String bytecode;

    @NotBlank(message = "ABI is required")
    private String abi;

    @NotBlank(message = "Environment is required")
    private String environment; // TESTNET, MAINNET

    private String version = "1.0.0";

    public DeployContractRequest() {
    }

    public DeployContractRequest(String name, String bytecode, String abi, String environment, String version) {
        this.name = name;
        this.bytecode = bytecode;
        this.abi = abi;
        this.environment = environment;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
