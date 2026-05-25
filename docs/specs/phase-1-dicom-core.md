# Phase 1 스펙 — DICOM 코어 + DIMSE 서버

> 상태: 확정 (구현 진행 중)
> 선행: Phase 0 기반 중 Spring Boot·Postgres·Docker Compose (완료)
> 관련: `docs/overview/00-overview.md`, `docs/plans/phase-1.md`

---

## 1. 목표

DICOM 영상이 DIMSE로 들어와서 파싱·저장·인덱싱되고, 다시 DIMSE로 검색·회수되는 **PACS 서버의 척추**를 완성한다.

**완료 데모 기준** (클라이언트 UI 없음 — dcm4che 명령행 도구 기준):
1. `echoscu`로 C-ECHO를 보내면 서버가 성공 응답한다.
2. `storescu`로 비압축 DICOM 파일을 C-STORE하면 파싱되어 원본이 스토리지에, 메타데이터가 DB에 저장된다.
3. `findscu`로 "환자 ID가 X인 스터디 목록"과 그 하위 시리즈·인스턴스를 C-FIND로 검색할 수 있다.
4. `getscu`로 스터디·시리즈·인스턴스를 C-GET하면 저장했던 파일과 SHA-256이 같은 파일을 받는다.
5. 위 전 과정이 자동화된 테스트로 검증된다 (TDD).

## 2. 범위

### 포함
- dcm4che 기반 파싱 계층 (래핑, 도메인 모델로 변환)
- Patient / Study / Series / Instance 4계층 메타데이터 인덱스 (Postgres)
- 원본 DICOM 파일 스토리지 (로컬 파일시스템, 경로 전략 포함), SHA-256 기록
- DIMSE SCP: C-ECHO, C-STORE, C-FIND(Study Root), C-GET(Study Root)
- association 수락 정책: 서버 AE Title, 호출 AE Title 허용 목록, 지원 SOP Class·전송구문
- 수신 검증: 필수 태그 존재, 지원 전송구문 확인, 중복 SOP Instance 처리

### 제외 (다른 Phase / 미래)
- 압축 전송구문 (JPEG*, RLE) — association 협상에서 수락하지 않는다
- C-MOVE, Patient Root 조회 모델, Modality Worklist (MWL은 Phase 3)
- DICOM TLS, 사용자 인증 (REST 인증은 Phase 3)
- WPF 클라이언트 (Phase 2), 오더·FHIR (Phase 3), 에이전트 (Phase 4)
- 오브젝트 스토리지(S3 등), 삭제·수정 기능

## 3. 도메인 모델

DICOM의 정보 계층을 그대로 반영한다. 각 계층의 키는 DICOM UID.

```
Patient (PatientID + IssuerOfPatientID‡, PatientName, BirthDate, Sex)
  └─ Study (StudyInstanceUID†, StudyDate/Time, AccessionNumber, StudyDescription)
       └─ Series (SeriesInstanceUID†, Modality, SeriesNumber, SeriesDescription)
            └─ Instance (SOPInstanceUID†, SOPClassUID, InstanceNumber,
                         Rows, Columns, BitsAllocated, PhotometricInterpretation,
                         TransferSyntaxUID, 원본 파일 경로, 파일 크기, SHA-256 해시)
```
† = 전역 유일 키 (DB unique 제약)
‡ = 복합 유일 키 (PatientID, IssuerOfPatientID)

- 필수 태그: PatientID, IssuerOfPatientID, StudyInstanceUID, SeriesInstanceUID, SOPInstanceUID, SOPClassUID. 영상 속성(Rows 등)은 SR 등 비영상 객체를 위해 nullable.
- 파싱 결과는 우리 도메인 객체로 변환하며, **dcm4che 타입(`Attributes` 등)은 `dicom`·`dimse` 패키지 밖으로 노출하지 않는다.**
- Patient는 (PatientID, IssuerOfPatientID) 기준으로 조회 후 없으면 생성한다.

## 4. 저장 설계

- **메타데이터 인덱스**: Postgres. 위 4계층을 각각 테이블로, 부모 FK 연결. 스키마 변경은 Flyway versioned migration으로 전진만 한다.
- **원본 파일**: 로컬 파일시스템. 경로 전략: `{storage-root}/{StudyInstanceUID}/{SeriesInstanceUID}/{SOPInstanceUID}.dcm`
- 저장 순서: 임시 파일에 쓰며 SHA-256 계산 → 최종 경로로 이동 → DB 인덱스 커밋. DB 실패 시 파일 롤백(삭제 또는 기존 파일 복원).
- **해시 기록**: 저장 시 SHA-256을 계산해 파일 크기와 함께 인덱스에 기록한다. 용도 — ① 사후 무결성 검증(디스크 손상·변조 탐지)의 기준값, ② C-GET 회수 결과 검증, ③ 백업·이관 검증. (검증 잡·통계 API 등 운영 기능 자체는 Phase 3 이후)
- **중복 SOP Instance 정책 — `SAME_SERIES`**:
  - 기존 Instance와 Study UID·Series UID가 모두 같으면 → 덮어쓴다(원본 파일 교체, Instance 메타데이터·해시 갱신).
  - Study UID 또는 Series UID가 다르면 → 충돌로 거절한다. 기존 Instance를 다른 계층으로 옮기거나 상위 계층 정보를 갱신하지 않는다.

## 5. DIMSE 서비스 설계

### 5.1 공통 — association 수락
- 서버 AE Title: `DCMIND` (설정값), 포트: `11112` (설정값).
- 호출 AE Title이 허용 목록에 없으면 association을 거절한다. 허용 목록은 설정 파일로 관리한다.
- 수락하는 전송구문: Implicit VR Little Endian, Explicit VR Little Endian. 그 외 전송구문만 제안된 presentation context는 거절한다.
- dcm4che-net(`Device`·`ApplicationEntity`·`Connection`) 구성은 Spring Bean 생명주기에 맞춰 시작·종료한다.

### 5.2 C-ECHO
- Verification SOP Class. 연결 확인용으로 항상 성공 응답.

### 5.3 C-STORE SCP — 수신
- 수락 SOP Class: 허용 목록으로 관리하는 Storage SOP Class (CT·MR·CR·DX·US·Secondary Capture·SR 계열 등). 정확한 목록은 단위 1-6 시작 시 확정.
- 수신 데이터셋 → 파싱 계층 → 저장 서비스 순서로 처리하고, 결과를 DIMSE 상태 코드로 응답한다.

### 5.4 C-FIND SCP — 검색
- Study Root Query/Retrieve Information Model – FIND.
- 조회 레벨: STUDY, SERIES, IMAGE.
- 매칭(표준의 실용적 부분집합): 단일 값, 와일드카드(`*`, `?`), 날짜 범위(StudyDate), UID 목록.
- 반환 속성: 3장 도메인 모델의 필드 중 요청 식별자에 포함된 키. 정확한 목록은 단위 1-7 시작 시 확정.
- 매칭 결과마다 Pending 응답, 마지막에 Success 응답. C-CANCEL 수신 시 중단.

### 5.5 C-GET SCP — 회수
- Study Root Query/Retrieve Information Model – GET.
- 회수 레벨: STUDY, SERIES, IMAGE.
- 같은 association 위에서 요청자에게 C-STORE 하위 작업으로 파일을 보낸다. 요청자는 해당 Storage SOP Class를 SCP 역할(Role Selection)로 제안해야 한다.
- 진행 중 Pending 응답에 남은·완료·실패·경고 하위 작업 수를 담는다.

## 6. 오류 처리 원칙

| 상황 | 동작 |
|---|---|
| 허용 목록에 없는 호출 AE Title | association 거절 |
| 미지원 전송구문 (압축) | 해당 presentation context 거절 |
| 미지원 SOP Class | 해당 presentation context 거절 |
| 필수 태그 누락 | C-STORE 실패 상태 + Error Comment에 누락 태그 명시 |
| 동일 SOPInstanceUID 재수신 (같은 Study·Series) | 덮어쓰기 후 성공 |
| 동일 SOPInstanceUID 재수신 (다른 Study 또는 Series) | C-STORE 실패 상태 + Error Comment에 충돌 UID 명시, 기존 데이터 유지 |
| 파일 저장 성공 후 DB 실패 | 파일 롤백, C-STORE 실패 상태 |
| C-FIND·C-GET 대상 없음 | 매칭 0건으로 Success |
| C-GET 하위 작업 일부 실패 | 최종 응답에 실패 수와 실패 SOP Instance UID 목록 |

- 구체적인 상태 코드 값은 각 단위 시작 시 DICOM PS3.4 Annex B·C와 PS3.7 Annex C를 확인해 확정하고, 계획 문서에 기록한다.

## 7. 테스트 전략 (TDD)

- 모든 기능은 **테스트 먼저** (Red → Green → Refactor).
- **유닛**: 파싱 계층 — 샘플 DICOM과 dcm4che로 합성한 최소 DICOM으로 도메인 객체 필드와 거절 케이스 검증.
- **통합 (저장)**: Spring 컨텍스트 + 실제 Postgres(Testcontainers) + 임시 스토리지 디렉터리.
- **통합 (DIMSE)**: 테스트 안에서 dcm4che-net SCU로 임의 포트에 뜬 SCP에 접속해 C-ECHO / C-STORE / C-FIND / C-GET 왕복을 검증한다. "C-STORE한 것이 C-FIND로 검색되고 C-GET으로 해시가 같은 파일로 회수된다"가 최종 통합 테스트다.
- 샘플 데이터: `test-data/`의 비식별화 DICOM(git 미추적) + 테스트 코드에서 합성 생성한 DICOM(경계 케이스용, git 추적).

## 8. 작업 단위 (요약 — 상세 절차는 `docs/plans/phase-1.md`)

| # | 단위 | 상태 | 완료 기준 |
|---|---|---|---|
| 1-1 | DICOM 데이터 모델 정리 | 완료 | — |
| 1-2 | Gradle 프로젝트 + dcm4che 파싱 확인 | 완료 | — |
| 1-3 | 도메인 모델 + 파싱 래퍼 | 완료 | 파싱 유닛 테스트 통과 |
| P0 | Spring Boot + Postgres + Docker Compose | 완료 | 앱 기동 + DB 연결 |
| 1-3R | 메타데이터·저장 계약 정렬 | 완료 | V5·V6 마이그레이션 포함 테스트 통과 |
| 1-4a | 메타데이터 계층 저장 + `SAME_SERIES` 정책 | 진행 중 | 저장 서비스 통합 테스트 통과 |
| 1-4b | 원본 파일 저장 + SHA-256 + 롤백 | 예정 | 파일·DB 일관성 통합 테스트 통과 |
| 1-5 | DIMSE 서버 기반 + C-ECHO | 예정 | `echoscu` 성공 |
| 1-6 | C-STORE SCP | 예정 | `storescu` 전송 → 저장 |
| 1-7 | C-FIND SCP | 예정 | `findscu` 검색 결과 |
| 1-8 | C-GET SCP | 예정 | **Phase 1 완료 데모** |

## 9. 열린 결정 (해당 단위 시작 시 확정)

- 수락 Storage SOP Class 목록 — 1-6
- C-FIND 반환 속성 목록 — 1-7
- 각 실패 상황의 DIMSE 상태 코드 값 — 1-6~1-8
