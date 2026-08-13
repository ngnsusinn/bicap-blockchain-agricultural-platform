package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SeasonExport;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Development adapter. Production must provide a VeChain implementation of the same gateway. */
@Component
@ConditionalOnProperty(name = "bicap.blockchain.export-mode", havingValue = "local", matchIfMissing = true)
public class LocalExportBlockchainGateway implements ExportBlockchainGateway {
    @Override public String recordExport(SeasonExport export) {
        try {
            String payload = export.getIdempotencyKey() + ":" + export.getSeasonId() + ":" + export.getQuantity();
            return "0x" + java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException("Cannot create local blockchain receipt", ex); }
    }
}
