package vn.courses.ut.edu.javaprogramming.bicap.dto;

public class BlockchainTransactionResponse {

    private String txId;
    private String status;
    private String message;
    private Long timestamp;

    public BlockchainTransactionResponse() {
    }

    public BlockchainTransactionResponse(String txId, String status, String message) {
        this.txId = txId;
        this.status = status;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public String getTxId() {
        return txId;
    }

    public void setTxId(String txId) {
        this.txId = txId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
