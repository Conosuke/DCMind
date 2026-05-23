package com.dcmind.store;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import com.dcmind.support.RepositoryTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
public class StudyRepositoryTest {

    // Spring Boot의 의존성 주입을 통해 리포지토리를 주입받음
    @Autowired PatientRepository patientRepository;
    @Autowired StudyRepository studyRepository;

    @Test
    void 스터디정보를_환자정보에_연결해_저장하고_조회한다() {

        // 테스트 환자 정보 생성
        String patientId = "TEST-0001";
        String patientName = "nux-lee";
        Patient patient = patientRepository.save(new Patient(patientId, patientName));

        // 테스트 스터디 정보 생성
        String studyInstanceUid = "1.2.3.4";
        studyRepository.save(new Study(studyInstanceUid, patient));

        Optional<Study> found = studyRepository.findByStudyInstanceUid(studyInstanceUid);

        assertThat(found).isPresent();
        assertThat(found.get().getPatient().getPatientId()).isEqualTo(patientId);
    }
}
