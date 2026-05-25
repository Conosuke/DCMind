package com.dcmind.support;

import org.dcm4che3.data.UID;

import com.dcmind.dicom.model.ParsedInstance;

// 테스트용 환자 정보 상수 클래스
public final class TestDicomFixture {

    public static final String TRANSFER_SYNTAX_UID = "1.2.840.10008.1.2.1";
    public static final Integer ROWS = 512;
    public static final Integer COLUMNS = 512;
    public static final Integer BITS_ALLOCATED = 16;

    public static final String PATIENT_ID = "TUX-1991";
    public static final String PATIENT_NAME = "Tux^Linux";
    public static final String ISSUER_OF_PATIENT_ID = "University^of^Helsinki";

    public static final String MODALITY = "CT";

    public static final String STUDY_INSTANCE_UID = "1.2.3";
    public static final String SERIES_INSTANCE_UID = "1.2.3.4";
    public static final String SOP_INSTANCE_UID = "1.2.3.4.5";
    public static final String SOP_CLASS_UID = UID.SecondaryCaptureImageStorage;

    public static ParsedInstance parsedInstance() {
        return new ParsedInstance(
            PATIENT_ID,
            ISSUER_OF_PATIENT_ID,
            PATIENT_NAME,
            STUDY_INSTANCE_UID,
            SERIES_INSTANCE_UID,
            SOP_INSTANCE_UID,
            SOP_CLASS_UID,
            MODALITY,
            TRANSFER_SYNTAX_UID,
            ROWS,
            COLUMNS,
            BITS_ALLOCATED
        );
    }

    private TestDicomFixture() {
    }
}
