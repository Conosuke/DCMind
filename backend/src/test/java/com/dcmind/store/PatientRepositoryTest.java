package com.dcmind.store;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import com.dcmind.support.RepositoryTest;
import com.dcmind.support.TestDicomFixture;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
public class PatientRepositoryTest {

    // Spring Boot의 의존성 주입을 통해 리포지토리를 주입받음
    @Autowired PatientRepository patientRepository;

    @Test
    void 환자를_저장하고_patientId와_Issuer로_조회() {

        patientRepository.save(new Patient(TestDicomFixture.PATIENT_ID, TestDicomFixture.ISSUER_OF_PATIENT_ID, TestDicomFixture.PATIENT_NAME));

        Optional<Patient> found = patientRepository.findByPatientIdAndIssuerOfPatientId(TestDicomFixture.PATIENT_ID, TestDicomFixture.ISSUER_OF_PATIENT_ID);

        assertThat(found).isPresent();
        assertThat(found.get().getPatientName()).isEqualTo(TestDicomFixture.PATIENT_NAME);
    }

    @Test
    void 같은_Patient라도_Issuer가_다르면_서로_다른_환자로_저장하고_조회() {

        String issuerOfPatientIdA = TestDicomFixture.ISSUER_OF_PATIENT_ID + "^A";
        String issuerOfPatientIdB = TestDicomFixture.ISSUER_OF_PATIENT_ID + "^B";

        Patient hospitalA = patientRepository.save(new Patient(TestDicomFixture.PATIENT_ID, issuerOfPatientIdA, TestDicomFixture.PATIENT_NAME + "^A"));
        Patient hospitalB = patientRepository.save(new Patient(TestDicomFixture.PATIENT_ID, issuerOfPatientIdB, TestDicomFixture.PATIENT_NAME + "^B"));

        Optional<Patient> foundHospitalA = patientRepository.findByPatientIdAndIssuerOfPatientId(TestDicomFixture.PATIENT_ID, issuerOfPatientIdA);
        Optional<Patient> foundHospitalB = patientRepository.findByPatientIdAndIssuerOfPatientId(TestDicomFixture.PATIENT_ID, issuerOfPatientIdB);

        assertThat(foundHospitalA).contains(hospitalA);
        assertThat(foundHospitalB).contains(hospitalB);

        assertThat(hospitalA.getId()).isNotEqualTo(hospitalB.getId());
    }
}
