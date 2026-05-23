package com.dcmind.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 스터디 정보 Repository
// JpaRepository를 상속받아 기본적인 CRUD 기능을 제공하며, Study Instance UID로 스터디 정보를 조회하는 메서드를 정의
public interface StudyRepository extends JpaRepository<Study, Long>{

    // study instance UID로 스터디 정보를 조회
    Optional<Study> findByStudyInstanceUid(String studyInstanceUid);
}
