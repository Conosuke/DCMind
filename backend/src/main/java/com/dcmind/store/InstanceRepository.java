package com.dcmind.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 인스턴스 정보 Repository
// JpaRepository를 상속받아 기본적인 CRUD 기능을 제공하며, Sop Instance UID로 스터디 정보를 조회하는 메서드를 정의
public interface InstanceRepository extends JpaRepository<Instance, Long>{

    // sop instance UID로 인스턴스 정보를 조회
    Optional<Instance> findBySopInstanceUid(String sopInstanceUid);
}
