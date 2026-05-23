package com.dcmind.store;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import com.dcmind.support.RepositoryTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
public class InstanceRepositoryTest {

    // Spring Boot의 의존성 주입을 통해 리포지토리를 주입받음
    @Autowired PatientRepository patientRepository;
    @Autowired StudyRepository studyRepository;
    @Autowired SeriesRepository seriesRepository;
    @Autowired InstanceRepository instanceRepository;

    @Test
    void 인스턴스를_전_계층에_연결해_저장하고_조회한다() {

        // 테스트 환자 정보 생성
        String patientId = "TEST-0001";
        String patientName = "nux-lee";
        Patient patient = patientRepository.save(new Patient(patientId, patientName));

        // 테스트 스터디 생성
        String studyInstanceUid = "1.2.3";
        Study study = studyRepository.save(new Study(studyInstanceUid, patient));

        // 테스트 시리즈 생성
        String seriesInstanceUid = "1.2.3.4";
        String modality = "CT";
        Series series = seriesRepository.save(new Series(seriesInstanceUid, modality, study));

        // 테스트 인스턴스 생성
        String sopInstanceUid = "1.2.3.4.5";
        String transferSyntaxUid = "1.2.840.10008.1.2.1";
        Integer rows = 512;
        Integer columns = 512;
        Integer bitsAllocated = 16;
        instanceRepository.save(new Instance(sopInstanceUid, series, rows, columns, bitsAllocated, transferSyntaxUid));

        Optional<Instance> found = instanceRepository.findBySopInstanceUid(sopInstanceUid);

        assertThat(found).isPresent();
        assertThat(found.get().getRows()).isEqualTo(512);
        assertThat(found.get().getSeries().getStudy().getPatient().getPatientId()).isEqualTo(patientId);
    }

}
