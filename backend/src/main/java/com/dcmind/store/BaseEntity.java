package com.dcmind.store;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import lombok.Getter;

// 엔티티의 공통 속성을 관리하는 Base 클래스.
// 합성 어노테이션은 jakarta, lombok의 제약때문에 공통으로 관리하지 못함.
@MappedSuperclass
@Getter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
