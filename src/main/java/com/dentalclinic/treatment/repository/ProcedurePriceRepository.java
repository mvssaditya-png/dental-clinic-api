package com.dentalclinic.treatment.repository;

import com.dentalclinic.treatment.entity.ProcedurePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcedurePriceRepository
        extends JpaRepository<ProcedurePrice, UUID> {

    @Query("""
        SELECT pp
        FROM ProcedurePrice pp
        WHERE pp.clinic.id = :clinicId
          AND pp.procedure.id = :procedureId
          AND pp.active = true
          AND pp.effectiveFrom <= :date
          AND (
                pp.effectiveTo IS NULL
                OR pp.effectiveTo >= :date
          )
        ORDER BY pp.effectiveFrom DESC
    """)
    List<ProcedurePrice> findActivePricesForDate(
            @Param("clinicId") UUID clinicId,
            @Param("procedureId") UUID procedureId,
            @Param("date") LocalDate date
    );

    default Optional<ProcedurePrice> findCurrentPrice(
            UUID clinicId,
            UUID procedureId,
            LocalDate date
    ) {
        return findActivePricesForDate(
                clinicId,
                procedureId,
                date
        )
                .stream()
                .findFirst();
    }

    @Query("""
        SELECT pp
        FROM ProcedurePrice pp
        WHERE pp.clinic.id = :clinicId
          AND pp.procedure.id = :procedureId
          AND pp.active = true
        ORDER BY pp.effectiveFrom DESC
    """)
    List<ProcedurePrice> findPriceHistory(
            @Param("clinicId") UUID clinicId,
            @Param("procedureId") UUID procedureId
    );

    @Query("""
        SELECT CASE WHEN COUNT(pp) > 0
                    THEN true
                    ELSE false
               END
        FROM ProcedurePrice pp
        WHERE pp.clinic.id = :clinicId
          AND pp.procedure.id = :procedureId
          AND pp.active = true
          AND pp.effectiveFrom <= :newEnd
          AND (
                pp.effectiveTo IS NULL
                OR pp.effectiveTo >= :newStart
          )
    """)
    boolean existsOverlappingPrice(
            @Param("clinicId")
            UUID clinicId,

            @Param("procedureId")
            UUID procedureId,

            @Param("newStart")
            LocalDate newStart,

            @Param("newEnd")
            LocalDate newEnd
    );
}