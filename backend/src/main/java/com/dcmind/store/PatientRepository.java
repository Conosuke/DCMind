package com.dcmind.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 환자 정보 Repository
// JpaRepository를 상속받아 기본적인 CRUD 기능을 제공하며, 환자 ID로 환자 정보를 조회하는 메서드를 정의
public interface PatientRepository extends JpaRepository<Patient, Long> {

    // 환자 ID로 환자 정보를 조회
    Optional<Patient> findByPatientIdAndIssuerOfPatientId(String patientId, String issuerOfPatientId);
}
