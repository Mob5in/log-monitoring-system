package ir.aut.logmonitor.api;

import ir.aut.logmonitor.alert.AlertRepository;
import ir.aut.logmonitor.api.dto.AlertResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Backend API subsystem: exposes alerts to external clients.
 */
@RestController
@RequestMapping("/alerts")
@Profile({"api", "default"})
public class AlertController {

    private final AlertRepository alertRepository;

    public AlertController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /**
     * Returns all alerts, most recent first, as JSON.
     */
    @GetMapping
    public List<AlertResponse> getAlerts() {
        return alertRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(AlertResponse::from)
                .toList();
    }
}