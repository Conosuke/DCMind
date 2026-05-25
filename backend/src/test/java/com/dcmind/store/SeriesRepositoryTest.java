package com.dcmind.store;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import com.dcmind.support.RepositoryTest;
import com.dcmind.support.TestDicomFixture;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
public class SeriesRepositoryTest {

    // Spring Boot의 의존성 주입을 통해 리포지토리를 주입받음
    @Autowired PatientRepository patientRepository;
    @Autowired StudyRepository studyRepository;
    @Autowired SeriesRepository seriesRepository;

    @Test
    void 시리즈를_스터디에_연결해_저장하고_조회한다() {

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
        seriesRepository.save(
            new Series(
                TestDicomFixture.SERIES_INSTANCE_UID,
                TestDicomFixture.MODALITY,
                study));

        Optional<Series> found = seriesRepository.findBySeriesInstanceUid(TestDicomFixture.SERIES_INSTANCE_UID);

        assertThat(found).isPresent();
        assertThat(found.get().getModality()).isEqualTo(TestDicomFixture.MODALITY);
        assertThat(found.get().getStudy().getStudyInstanceUid()).isEqualTo(TestDicomFixture.STUDY_INSTANCE_UID);
    }
}
