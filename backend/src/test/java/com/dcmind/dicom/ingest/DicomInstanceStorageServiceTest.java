package com.dcmind.dicom.ingest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.dcm4che3.data.UID;

import com.dcmind.dicom.model.ParsedInstance;
import com.dcmind.store.Instance;
import com.dcmind.store.InstanceRepository;
import com.dcmind.store.PatientRepository;
import com.dcmind.store.SeriesRepository;
import com.dcmind.store.StudyRepository;
import com.dcmind.support.RepositoryTest;
import com.dcmind.support.TestDicomFixture;

import static org.assertj.core.api.Assertions.assertThat;

@RepositoryTest
public class DicomInstanceStorageServiceTest {
    
    @Autowired DicomInstanceStorageService storageService;
    @Autowired PatientRepository patientRepository;
    @Autowired StudyRepository studyRepository;
    @Autowired SeriesRepository seriesRepository;
    @Autowired InstanceRepository instanceRepository;

    @Test
    void 같은_Study와_Series의_동일_SOP_인스턴스는_메타데이터를_덮어쓴다() {
        storageService.store(TestDicomFixture.parsedInstance());

        ParsedInstance reuploaded = new ParsedInstance(
            TestDicomFixture.PATIENT_ID,
            TestDicomFixture.ISSUER_OF_PATIENT_ID,
            TestDicomFixture.PATIENT_NAME,
            TestDicomFixture.STUDY_INSTANCE_UID,
            TestDicomFixture.SERIES_INSTANCE_UID,
            TestDicomFixture.SOP_INSTANCE_UID,
            TestDicomFixture.SOP_CLASS_UID,
            TestDicomFixture.MODALITY,
            UID.ImplicitVRLittleEndian,
            1024,
            TestDicomFixture.COLUMNS,
            TestDicomFixture.BITS_ALLOCATED
        );

        storageService.store(reuploaded);

        Instance stored = instanceRepository
            .findBySopInstanceUid(TestDicomFixture.SOP_INSTANCE_UID)
            .orElseThrow();

        assertThat(instanceRepository.count()).isEqualTo(1);
        assertThat(stored.getRows()).isEqualTo(1024);
        assertThat(stored.getTransferSyntaxUid()).isEqualTo(UID.ImplicitVRLittleEndian);
}

    @Test
    void 새_파싱_인스턴스를_계층에_저장() {

        storageService.store(TestDicomFixture.parsedInstance());

        assertThat(
            patientRepository.findByPatientIdAndIssuerOfPatientId(TestDicomFixture.PATIENT_ID, TestDicomFixture.ISSUER_OF_PATIENT_ID)
        ).isPresent();

        assertThat(
            studyRepository.findByStudyInstanceUid(TestDicomFixture.STUDY_INSTANCE_UID)
        ).isPresent();

        assertThat(
            seriesRepository.findBySeriesInstanceUid(TestDicomFixture.SERIES_INSTANCE_UID)
        ).isPresent();

        assertThat(
            instanceRepository.findBySopInstanceUid(TestDicomFixture.SOP_INSTANCE_UID)
        ).isPresent();
    }
}
