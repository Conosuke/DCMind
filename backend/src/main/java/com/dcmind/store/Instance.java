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

    @Column(name = "sop_class_uid")
    private String sopClassUid;

    public Instance(String sopInstanceUid, String sopClassUid, Integer rows, Integer columns,
                    Integer bitsAllocated, String transferSyntaxUid, Series series) {
        this.sopInstanceUid = sopInstanceUid;
        this.sopClassUid = sopClassUid;
        this.series = series;
        this.rows = rows;
        this.columns = columns;
        this.bitsAllocated = bitsAllocated;
        this.transferSyntaxUid = transferSyntaxUid;
    }

    // 같은 Study·Series로 재전송된 인스턴스의 메타데이터를 새 값으로 덮어쓴다(SAME_SERIES 정책).
    // SOP Instance UID와 소속 Series는 바꾸지 않는다.
    public void overwriteMetadata(String sopClassUid, int rows, int columns,
                                  int bitsAllocated, String transferSyntaxUid) {
        this.sopClassUid = sopClassUid;
        this.rows = rows;
        this.columns = columns;
        this.bitsAllocated = bitsAllocated;
        this.transferSyntaxUid = transferSyntaxUid;
    }
}
