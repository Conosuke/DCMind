package com.dcmind.store;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

// patient 테이블과 매핑되는 엔티티클래스
@Entity // JPA 엔티티로 매핑
@Table(name = "patient") // patient 테이블과 매핑
@Getter // Lombok: getter 자동 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Lombok: JPA용 기본 생성자(protected) 자동 생성
public class Patient extends BaseEntity {

    @Column(name = "patient_id", nullable = false, unique = true)
    private String patientId;

    @Column(name = "patient_name")
    private String patientName;

    public Patient(String patientId, String patientName) {
        this.patientId = patientId;
        this.patientName = patientName;
    }
}
