package ir.aut.logmonitor.alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Alert}.
 *
 * Extending JpaRepository automatically gives us save(), findById(),
 * findAll(), delete(), etc. — no implementation needed, Spring generates
 * it at runtime.
 */
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /**
     * Fetches all alerts ordered by creation time, most recent first.
     * Spring Data derives the query automatically from the method name.
     * Used by the Backend API to serve alerts "in time order" as required.
     */
    List<Alert> findAllByOrderByCreatedAtDesc();
}