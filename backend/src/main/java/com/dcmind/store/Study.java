package com.dcmind.store;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity // JPA 엔티티로 매핑
@Table(name = "study") // study 테이블과 매핑
@Getter // Lombok: getter 자동 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Lombok: JPA용 기본 생성자(protected) 자동 생성
public class Study extends BaseEntity {

    @Column(name = "study_instance_uid", nullable = false, unique = true)
    private String studyInstanceUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    public Study(String studyInstanceUid, Patient patient) {
        this.studyInstanceUid = studyInstanceUid;
        this.patient = patient;
    }
}
