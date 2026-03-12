package com.company.common.security.repository;

import com.company.common.security.entity.PwdHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PwdHistoryRepository extends JpaRepository<PwdHistory, Long> {

    /**
     * Finds the most recent password history records for a user.
     * Used to prevent password reuse (HIPAA, PCI-DSS compliance).
     *
     * @param userId the user ID
     * @param pageable pageable to limit results (e.g., last 5 passwords)
     * @return list of recent password hashes, ordered by creation date descending
     */
    @Query("SELECT ph FROM PwdHistory ph WHERE ph.userId = :userId ORDER BY ph.createdDate DESC")
    List<PwdHistory> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
}
