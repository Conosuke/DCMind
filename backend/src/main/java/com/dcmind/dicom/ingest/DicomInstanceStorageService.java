package com.dcmind.dicom.ingest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dcmind.dicom.model.ParsedInstance;
import com.dcmind.store.Instance;
import com.dcmind.store.InstanceRepository;
import com.dcmind.store.Patient;
import com.dcmind.store.PatientRepository;
import com.dcmind.store.Series;
import com.dcmind.store.SeriesRepository;
import com.dcmind.store.Study;
import com.dcmind.store.StudyRepository;

@Service // 이 클래스를 애플리케이션 서비스 Bean으로 등록.
public class DicomInstanceStorageService {
    
    // 리포지토리 인스턴스들
    private final PatientRepository patientRepository;
    private final StudyRepository studyRepository;
    private final SeriesRepository seriesRepository;
    private final InstanceRepository instanceRepository;

    // 생성자: 의존성 주입을 통해 리포지토리 인스턴스를 초기화
    public DicomInstanceStorageService(
        PatientRepository patientRepository,
        StudyRepository studyRepository,
        SeriesRepository seriesRepository,
        InstanceRepository instanceRepository
    ) {
        this.patientRepository = patientRepository;
        this.studyRepository = studyRepository;
        this.seriesRepository = seriesRepository;
        this.instanceRepository = instanceRepository;
    }

    @Transactional // 메서드 실행 중 발생하는 모든 데이터베이스 작업을 하나의 트랜잭션으로 묶음. 예외 발생 시 롤백.
    public Instance store(ParsedInstance parsedInstance) {

        // 환자 정보 조회 또는 생성
        Patient patient = patientRepository
            .findByPatientIdAndIssuerOfPatientId(parsedInstance.patientId(), parsedInstance.issuerOfPatientId())
            .orElseGet(() -> patientRepository.save(
                new Patient(
                    parsedInstance.patientId(),
                    parsedInstance.issuerOfPatientId(),
                    parsedInstance.patientName()
                )
            ));
        
        // 스터디 정보 조회 또는 생성
        Study study = studyRepository
            .findByStudyInstanceUid(parsedInstance.studyInstanceUid())
            .orElseGet(() -> studyRepository.save(
                new Study(
                    parsedInstance.studyInstanceUid(),
                    patient
                )
            ));
        
        // 시리즈 정보 조회 또는 생성
        Series series = seriesRepository
            .findBySeriesInstanceUid(parsedInstance.seriesInstanceUid())
            .orElseGet(() -> seriesRepository.save(
                new Series(
                    parsedInstance.seriesInstanceUid(),
                    parsedInstance.modality(),
                    study
                )
            ));

        // 인스턴스 정보 생성
        Instance instance = new Instance(
            parsedInstance.sopInstanceUid(),
            parsedInstance.sopClassUid(),
            parsedInstance.rows(),
            parsedInstance.columns(),
            parsedInstance.bitsAllocated(),
            parsedInstance.transferSyntaxUid(),
            series
        );

        // 인스턴스 정보 저장 후 저장된 인스턴스 반환
        return instanceRepository.save(instance);
    }
}
