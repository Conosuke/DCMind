package com.dcmind.store;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity // JPA 엔티티로 매핑
@Table(name = "series") // series 테이블과 매핑
@Getter // Lombok: getter 자동 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Lombok: JPA용 기본 생성자(protected) 자동 생성
public class Series extends BaseEntity {

    @Column(name = "series_instance_uid", nullable = false, unique = true)
    private String seriesInstanceUid;

    @Column(name = "modality")
    private String modality;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id")
    private Study study;

    public Series(String seriesInstanceUid, String modality, Study study) {
        this.seriesInstanceUid = seriesInstanceUid;
        this.modality = modality;
        this.study = study;
    }
}
