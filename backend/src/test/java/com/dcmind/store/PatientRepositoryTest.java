package com.dcmind.store;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import com.dcmind.support.RepositoryTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
public class PatientRepositoryTest {

    // Spring Boot의 의존성 주입을 통해 리포지토리를 주입받음
    @Autowired PatientRepository patientRepository;

    @Test
    void 환자를_저장하고_patientId로_조회() {

        // 테스트 환자 정보 생성
        String testId = "TEST-0001";
        String testName = "nux-lee";
        patientRepository.save(new Patient(testId, testName));

        Optional<Patient> found = patientRepository.findByPatientId(testId);

        assertThat(found).isPresent();
        assertThat(found.get().getPatientName()).isEqualTo(testName);
    }
}
