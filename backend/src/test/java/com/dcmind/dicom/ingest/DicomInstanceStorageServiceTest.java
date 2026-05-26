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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void 다른_Series로_들어온_동일_SOP_인스턴스는_거절하고_기존_계층을_유지한다() {
        storageService.store(TestDicomFixture.parsedInstance());

        ParsedInstance conflicting = reuploadedTo(TestDicomFixture.STUDY_INSTANCE_UID, OTHER_SERIES_INSTANCE_UID);

        assertThatThrownBy(() -> storageService.store(conflicting))
            .isInstanceOf(DuplicateSopInstanceException.class)
            .hasMessageContaining(TestDicomFixture.SOP_INSTANCE_UID);

        assertExistingHierarchyUnchanged();
    }

    @Test
    void 다른_Study로_들어온_동일_SOP_인스턴스는_거절하고_기존_계층을_유지한다() {
        storageService.store(TestDicomFixture.parsedInstance());

        ParsedInstance conflicting = reuploadedTo(OTHER_STUDY_INSTANCE_UID, TestDicomFixture.SERIES_INSTANCE_UID);

        assertThatThrownBy(() -> storageService.store(conflicting))
            .isInstanceOf(DuplicateSopInstanceException.class)
            .hasMessageContaining(TestDicomFixture.SOP_INSTANCE_UID);

        assertExistingHierarchyUnchanged();
    }

    private static final String OTHER_STUDY_INSTANCE_UID = "9.8.7";
    private static final String OTHER_SERIES_INSTANCE_UID = "9.8.7.6";

    // 같은 SOP Instance UID를 지정한 Study·Series로, 다른 Rows로 재전송한 인스턴스
    private static ParsedInstance reuploadedTo(String studyInstanceUid, String seriesInstanceUid) {
        return new ParsedInstance(
            TestDicomFixture.PATIENT_ID,
            TestDicomFixture.ISSUER_OF_PATIENT_ID,
            TestDicomFixture.PATIENT_NAME,
            studyInstanceUid,
            seriesInstanceUid,
            TestDicomFixture.SOP_INSTANCE_UID,
            TestDicomFixture.SOP_CLASS_UID,
            TestDicomFixture.MODALITY,
            TestDicomFixture.TRANSFER_SYNTAX_UID,
            1024,
            TestDicomFixture.COLUMNS,
            TestDicomFixture.BITS_ALLOCATED
        );
    }

    // 충돌 거절 후: 새 Study·Series가 생기지 않고, 기존 Instance의 소속과 메타데이터가 그대로다.
    private void assertExistingHierarchyUnchanged() {
        assertThat(studyRepository.findByStudyInstanceUid(OTHER_STUDY_INSTANCE_UID)).isEmpty();
        assertThat(seriesRepository.findBySeriesInstanceUid(OTHER_SERIES_INSTANCE_UID)).isEmpty();

        Instance stored = instanceRepository
            .findBySopInstanceUid(TestDicomFixture.SOP_INSTANCE_UID)
            .orElseThrow();

        assertThat(instanceRepository.count()).isEqualTo(1);
        assertThat(stored.getSeries().getSeriesInstanceUid()).isEqualTo(TestDicomFixture.SERIES_INSTANCE_UID);
        assertThat(stored.getSeries().getStudy().getStudyInstanceUid()).isEqualTo(TestDicomFixture.STUDY_INSTANCE_UID);
        assertThat(stored.getRows()).isEqualTo(TestDicomFixture.ROWS);
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
