package ir.aut.logmonitor.api.dto;

import ir.aut.logmonitor.alert.Alert;

import java.time.LocalDateTime;

/**
 * The JSON shape returned by the Backend API for a single alert.
 *
 * Kept separate from the {@link Alert} JPA entity on purpose: the API's
 * response format shouldn't be tightly coupled to the database schema.
 * If the Alert entity changes later, this response shape doesn't have to.
 */
public record AlertResponse(
        Long id,
        String ruleName,
        String componentName,
        String description,
        LocalDateTime createdAt
) {
    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getRuleName(),
                alert.getComponentName(),
                alert.getDescription(),
                alert.getCreatedAt()
        );
    }
}