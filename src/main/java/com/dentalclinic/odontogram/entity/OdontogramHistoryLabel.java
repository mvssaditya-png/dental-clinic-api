package com.dentalclinic.odontogram.entity;
import jakarta.persistence.*;
import lombok.Getter;
import java.util.UUID;
@Entity @Table(name="odontogram_history_label") @Getter
@org.hibernate.annotations.Immutable
public class OdontogramHistoryLabel {
 @Id @Column(name="history_id") private UUID historyId;
 private String previousName; private String previousCode; private String newName; private String newCode;
 private String source;
}
