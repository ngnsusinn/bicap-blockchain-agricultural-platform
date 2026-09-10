package vn.courses.ut.edu.javaprogramming.bicap.dto;

/**
 * Internal admin note for a farm (BICAP-4 / SRS-ADM-003).
 * Optional free text, up to 2000 characters — the service trims the value first and
 * validates the trimmed length, so surrounding whitespace never triggers a false 400.
 */
public class FarmNotesUpdateRequest {

    private String notes;

    public FarmNotesUpdateRequest() {}

    public FarmNotesUpdateRequest(String notes) {
        this.notes = notes;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
