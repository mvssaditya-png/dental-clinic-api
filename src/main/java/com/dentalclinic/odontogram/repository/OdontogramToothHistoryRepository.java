package com.dentalclinic.odontogram.repository;

import com.dentalclinic.odontogram.entity.OdontogramToothHistory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OdontogramToothHistoryRepository
        extends JpaRepository<OdontogramToothHistory, UUID> {

    List<OdontogramToothHistory>
    findAllByOdontogramIdAndToothNumberOrderByChangedAtDesc(
            UUID odontogramId,
            String toothNumber
    );
}