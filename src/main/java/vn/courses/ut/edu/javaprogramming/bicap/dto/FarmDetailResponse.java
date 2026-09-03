package vn.courses.ut.edu.javaprogramming.bicap.dto;

import java.util.List;

/**
 * Full registration profile of a farm shown in the admin review panel (BICAP-3 / SRS-ADM-002).
 * Extends the summary {@link FarmResponse} with the list of certification documents and,
 * for farm management (BICAP-4 / SRS-ADM-003), the season history of the farm.
 */
public class FarmDetailResponse extends FarmResponse {
    private List<FarmCertificationResponse> certifications;
    private List<SeasonResponse> seasons;

    public FarmDetailResponse() {}

    public FarmDetailResponse(FarmResponse summary, List<FarmCertificationResponse> certifications) {
        this(summary, certifications, null);
    }

    public FarmDetailResponse(FarmResponse summary, List<FarmCertificationResponse> certifications,
                              List<SeasonResponse> seasons) {
        super(summary.getId(), summary.getName(), summary.getAddress(), summary.getArea(),
                summary.getGpsLat(), summary.getGpsLng(),
                summary.getDescription(), summary.getProductTypes(), summary.getAdminNotes(),
                summary.getStatus(), summary.getCreatedAt(), summary.getUpdatedAt(),
                summary.getOwnerName(), summary.getOwnerEmail(), summary.getOwnerPhone(),
                summary.getCertificationCount());
        this.certifications = certifications;
        this.seasons = seasons;
    }

    public List<FarmCertificationResponse> getCertifications() { return certifications; }
    public void setCertifications(List<FarmCertificationResponse> certifications) { this.certifications = certifications; }
    public List<SeasonResponse> getSeasons() { return seasons; }
    public void setSeasons(List<SeasonResponse> seasons) { this.seasons = seasons; }
}
