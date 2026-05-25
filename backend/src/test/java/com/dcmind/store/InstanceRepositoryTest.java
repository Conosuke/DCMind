package com.dcmind.store;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import com.dcmind.support.RepositoryTest;
import com.dcmind.support.TestDicomFixture;

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

        // 테스트 환자 생성
        Patient patient = patientRepository.save(
            new Patient(
                TestDicomFixture.PATIENT_ID,
                TestDicomFixture.ISSUER_OF_PATIENT_ID,
                TestDicomFixture.PATIENT_NAME));

        // 테스트 스터디 생성
        Study study = studyRepository.save(
            new Study(
                TestDicomFixture.STUDY_INSTANCE_UID,
                patient));

        // 테스트 시리즈 생성
        Series series = seriesRepository.save(
            new Series(
                TestDicomFixture.SERIES_INSTANCE_UID,
                TestDicomFixture.MODALITY,
                study));

        // 테스트 인스턴스 생성
        instanceRepository.save(
            new Instance(
                TestDicomFixture.SOP_INSTANCE_UID,
                TestDicomFixture.SOP_CLASS_UID,
                TestDicomFixture.ROWS,
                TestDicomFixture.COLUMNS,
                TestDicomFixture.BITS_ALLOCATED,
                TestDicomFixture.TRANSFER_SYNTAX_UID,
                series));

        Optional<Instance> found = instanceRepository.findBySopInstanceUid(TestDicomFixture.SOP_INSTANCE_UID);

        assertThat(found).isPresent();
        assertThat(found.get().getRows()).isEqualTo(512);
        assertThat(found.get().getSopClassUid()).isEqualTo(TestDicomFixture.SOP_CLASS_UID);
        assertThat(found.get().getSeries().getStudy().getPatient().getPatientId()).isEqualTo(TestDicomFixture.PATIENT_ID);
    }

}
