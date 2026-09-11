package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vn.courses.ut.edu.javaprogramming.bicap.entity.SeasonExport;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Dev-only offline adapter — never selected unless {@code bicap.blockchain.export-mode=local}
 * is set explicitly.
 *
 * <p>It does NOT touch any blockchain: it returns a deterministic SHA-256 digest of the
 * export fields. It used to be the {@code matchIfMissing} default, which silently made the
 * "saved on blockchain" claim false for every export; the default is now
 * {@link VeChainExportBlockchainGateway}.
 */
@Component
@ConditionalOnProperty(name = "bicap.blockchain.export-mode", havingValue = "local")
public class LocalExportBlockchainGateway implements ExportBlockchainGateway {
    @Override public String recordExport(SeasonExport export) {
        try {
            String payload = export.getIdempotencyKey() + ":" + export.getSeasonId() + ":" + export.getQuantity();
            return "0x" + java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) { throw new IllegalStateException("Cannot create local blockchain receipt", ex); }
    }

    @Override public boolean isLiveAnchor() { return false; }
}
