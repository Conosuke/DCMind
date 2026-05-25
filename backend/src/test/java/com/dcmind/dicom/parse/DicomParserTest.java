package com.dcmind.dicom.parse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.dcmind.dicom.model.ParsedInstance;
import com.dcmind.support.TestDicomFixture;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// DICOM 파서 기능 테스트
class DicomParserTest {

    @ParameterizedTest
    @ValueSource(strings = { UID.ExplicitVRLittleEndian, UID.ImplicitVRLittleEndian })
    void 비압축_DICOM_파일_파싱_테스트(String expectedTransferSyntax) throws Exception {

        // DICOM 파서 생성
        DicomParser dicomParser = new DicomParser();

        // 테스트용 DICOM 데이터 생성
        try (InputStream input =
            createDicom(
                expectedTransferSyntax,
                dataset -> {
                    dataset.setString(Tag.PatientName, VR.PN, TestDicomFixture.PATIENT_NAME);
                    dataset.setString(Tag.Modality, VR.CS, TestDicomFixture.MODALITY);
                    dataset.setInt(Tag.Rows, VR.US, TestDicomFixture.ROWS);
                    dataset.setInt(Tag.Columns, VR.US, TestDicomFixture.COLUMNS);
                    dataset.setInt(Tag.BitsAllocated, VR.US, TestDicomFixture.BITS_ALLOCATED);
                }
            )
        ) {

            // 파싱된 인스턴스 추출
            ParsedInstance instance = dicomParser.parse(input);

            // 사전에 정의된 값과 비교하여 검증
            assertEquals(TestDicomFixture.PATIENT_ID, instance.patientId());
            assertEquals(TestDicomFixture.ISSUER_OF_PATIENT_ID, instance.issuerOfPatientId());
            assertEquals(TestDicomFixture.PATIENT_NAME, instance.patientName());
            assertEquals(TestDicomFixture.MODALITY, instance.modality());
            assertEquals(expectedTransferSyntax, instance.transferSyntaxUid());
            assertEquals(TestDicomFixture.STUDY_INSTANCE_UID, instance.studyInstanceUid());
            assertEquals(TestDicomFixture.SERIES_INSTANCE_UID, instance.seriesInstanceUid());
            assertEquals(TestDicomFixture.SOP_INSTANCE_UID, instance.sopInstanceUid());
            assertEquals(TestDicomFixture.ROWS, instance.rows());
            assertEquals(TestDicomFixture.COLUMNS, instance.columns());
            assertEquals(TestDicomFixture.BITS_ALLOCATED, instance.bitsAllocated());
            assertEquals(TestDicomFixture.SOP_CLASS_UID, instance.sopClassUid());
        }
    }

    @ParameterizedTest(name = "{2} 태그가 누락되면 거절한다")
    @MethodSource("requiredTagCases")
    void 필수_공통_태그가_누락되면_거절한다(int tag, VR vr, String tagName) throws Exception {

        // DICOM 파서 생성
        DicomParser dicomParser = new DicomParser();

        try (InputStream input = createDicom(dataset -> dataset.remove(tag))) {

            MissingRequiredTagException exception = assertThrows(
                MissingRequiredTagException.class,
                () -> dicomParser.parse(input)
            );

            assertTrue(exception.getMessage().contains(tagName));
        }
    }

    @ParameterizedTest(name = "{2} 태그가 공백이면 거절한다")
    @MethodSource("requiredTagCases")
    void 필수_공통_태그가_공백이면_거절한다(int tag, VR vr, String tagName) throws Exception {

        // DICOM 파서 생성
        DicomParser dicomParser = new DicomParser();

        try (InputStream input = createDicom(dataset -> dataset.setString(tag, vr, " "))) {

            MissingRequiredTagException exception = assertThrows(
                MissingRequiredTagException.class,
                () -> dicomParser.parse(input)
            );

            assertTrue(exception.getMessage().contains(tagName));
        }
    }

    @Test
    void DICOM이_아닌_파일은_거절() throws Exception {

        // DICOM 파서 생성
        DicomParser dicomParser = new DicomParser();

        // 문자열 바이트를 입력으로 생성
        InputStream input = new ByteArrayInputStream("DICOM 파일이 아닌 문자열 바이트".getBytes());

        assertThrows(
            NotADicomFileException.class,
            () -> dicomParser.parse(input)
        );
    }

    @Test
    void 미지원_압축_전송구문은_거절() throws Exception {

        // DICOM 파서 생성
        DicomParser dicomParser = new DicomParser();

        try (InputStream input = getClass().getResourceAsStream("/dicom/penguin-jpeg-baseline.dcm")) {

            assertThrows(
                UnsupportedTransferSyntaxException.class,
                () -> dicomParser.parse(input)
            );
        }
    }

    private static Stream<Arguments> requiredTagCases() {

        return Stream.of(
            Arguments.of(Tag.PatientID, VR.LO, "PatientID"),
            Arguments.of(Tag.IssuerOfPatientID, VR.LO, "IssuerOfPatientID"),
            Arguments.of(Tag.StudyInstanceUID, VR.UI, "StudyInstanceUID"),
            Arguments.of(Tag.SeriesInstanceUID, VR.UI, "SeriesInstanceUID"),
            Arguments.of(Tag.SOPInstanceUID, VR.UI, "SOPInstanceUID"),
            Arguments.of(Tag.SOPClassUID, VR.UI, "SOPClassUID")
        );
    }

    private static InputStream createDicom(Consumer<Attributes> customize) throws Exception {
        return createDicom(UID.ExplicitVRLittleEndian, customize);
    }

    private static InputStream createDicom(String transferSyntaxUID, Consumer<Attributes> customize) throws Exception {

        Attributes dataset = new Attributes();
        dataset.setString(Tag.PatientID, VR.LO, TestDicomFixture.PATIENT_ID);
        dataset.setString(Tag.IssuerOfPatientID, VR.LO, TestDicomFixture.ISSUER_OF_PATIENT_ID);
        dataset.setString(Tag.StudyInstanceUID, VR.UI, TestDicomFixture.STUDY_INSTANCE_UID);
        dataset.setString(Tag.SeriesInstanceUID, VR.UI, TestDicomFixture.SERIES_INSTANCE_UID);
        dataset.setString(Tag.SOPInstanceUID, VR.UI, TestDicomFixture.SOP_INSTANCE_UID);
        dataset.setString(Tag.SOPClassUID, VR.UI, TestDicomFixture.SOP_CLASS_UID);

        Attributes fileMetaInformation = dataset.createFileMetaInformation(transferSyntaxUID);

        // 파일 메타정보를 만든 뒤 Dataset만 변경.
        // 이 과정으로 SOP Class UID 누락 테스트에서도 유효한 파일 헤더가 유지된다.
        customize.accept(dataset);

        // DICOM 파일 생성
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (DicomOutputStream dicomOutput = new DicomOutputStream(output, UID.ExplicitVRLittleEndian)) {
            dicomOutput.writeDataset(fileMetaInformation, dataset);
        }

        return new ByteArrayInputStream(output.toByteArray());
    }
}
