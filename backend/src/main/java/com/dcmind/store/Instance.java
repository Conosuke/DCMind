package com.dcmind.store;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity // JPA 엔티티로 매핑
@Table(name = "instance") // instance 테이블과 매핑
@Getter // Lombok: getter 자동 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Lombok: JPA용 기본 생성자(protected) 자동 생성
public class Instance extends BaseEntity {

    @Column(name = "sop_instance_uid", nullable = false, unique = true)
    private String sopInstanceUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "series_id")
    private Series series;

    @Column(name = "rows")
    private Integer rows;

    @Column(name = "columns")
    private Integer columns;

    @Column(name = "bits_allocated")
    private Integer bitsAllocated;

    @Column(name = "transfer_syntax_uid")
    private String transferSyntaxUid;

    public Instance(String sopInstanceUid, Series series, Integer rows, Integer columns,
                    Integer bitsAllocated, String transferSyntaxUid) {
        this.sopInstanceUid = sopInstanceUid;
        this.series = series;
        this.rows = rows;
        this.columns = columns;
        this.bitsAllocated = bitsAllocated;
        this.transferSyntaxUid = transferSyntaxUid;
    }
}
