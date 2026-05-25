/*
  PatientID 단독 유니크 제약을 (PatientID, IssuerOfPatientID) 복합 유니크 제약으로 변경한다.

  DICOM PatientID는 기관 내부에서만 유일할 수 있으므로, 여러 기관의 데이터를 함께 저장할 때
  IssuerOfPatientID 없이 PatientID만으로 환자를 식별하면 충돌할 수 있다.

  기존 patient 행에는 원본 Issuer 정보가 없으므로 DCMIND-LEGACY를 부여한다.
  이 값은 기존 데이터가 이관 전 시스템에서 들어왔음을 나타내며, 실제 의료기관 Issuer를 추정하지 않는다.
*/
ALTER TABLE patient ADD COLUMN issuer_of_patient_id VARCHAR(64);

UPDATE patient
SET issuer_of_patient_id = 'DCMIND-LEGACY';

ALTER TABLE patient ALTER COLUMN issuer_of_patient_id SET NOT NULL;

ALTER TABLE patient DROP CONSTRAINT patient_patient_id_key;

ALTER TABLE patient ADD CONSTRAINT uk_patient_patient_id_issuer
    UNIQUE (patient_id, issuer_of_patient_id);
