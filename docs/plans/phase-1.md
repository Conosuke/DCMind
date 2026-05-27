# Phase 1 구현 계획 — DICOM 코어 + DIMSE 서버

> 스펙: `docs/specs/phase-1-dicom-core.md`
> 진행 방식: 각 단위는 로컬 `CLAUDE.md`의 **작업 단위 학습 루프**로 진행한다 — 개념 브리핑 → 테스트 리뷰(Red) → 구현(Green → Refactor) → 코드 리뷰 가이드 → 새 개념 정리 → 질의응답 → 이해도 점검. 에이전트가 구현하고, 사용자는 설계 결정·리뷰·점검으로 학습한다.
>
> 진행 상태: 1-1~1-3, P0, 1-3R, 1-4a 완료. 다음 단위는 1-4b.

---

## 완료된 단위

- **1-1 DICOM 데이터 모델 정리**: 파일 구조, 태그·VR, 전송구문, Patient→Study→Series→Instance 계층과 UID.
- **1-2 Gradle 프로젝트 + dcm4che 파싱 확인**: `backend/` Gradle 프로젝트, 샘플 파일에서 주요 태그 추출.
- **1-3 도메인 모델 + 파싱 래퍼**: `ParsedInstance`, `DicomParser`. 비DICOM·필수 태그 누락·미지원 전송구문 거절.
- **P0 Spring Boot + Postgres + Docker Compose**: Spring Boot 앱, Flyway V1~V4, Testcontainers 기반 리포지토리 테스트.
- **1-3R 메타데이터·저장 계약 정렬**: 필수 태그 6종 검증, `IssuerOfPatientID`·`SOPClassUID` 추출, V5(patient issuer)·V6(instance SOP Class UID) 마이그레이션, 테스트 fixture(`TestDicomFixture`) 추출.
- **1-4a 메타데이터 계층 저장 + `SAME_SERIES` 정책**: `DicomInstanceStorageService.store(ParsedInstance)`가 Patient·Study·Series를 find-or-create하고 Instance를 저장한다. 중복 SOP Instance는 상위 계층 생성 전에 판정해, 같은 Study·Series면 Instance 메타데이터를 덮어쓰고 다르면 `DuplicateSopInstanceException`으로 거절한다.

---

## 단위 1-4b — 원본 파일 저장 + SHA-256 + 롤백

- **무엇을**: 파일 스토리지 서비스(경로 전략·임시 파일→최종 경로 이동), 저장 중 SHA-256·파일 크기 계산, Instance에 경로·크기·해시 기록(새 마이그레이션), DB 실패 시 파일 롤백. `SAME_SERIES` 덮어쓰기 시 파일 교체와 롤백 시 기존 파일 복원.
- **왜**: 원본 없이 메타데이터만 있으면 C-GET으로 회수할 수 없다. 파일과 DB의 일관성이 PACS 무결성의 기준이다.
- **학습 포인트**: 파일 I/O(`java.nio.file`), 임시 파일→원자적 이동, `MessageDigest`(SHA-256), 파일시스템과 DB 트랜잭션의 불일치 문제와 보상(롤백) 처리, 트랜잭션 동기화.
- **진행 순서**: ① 파일 스토리지 단위 테스트(경로 전략, 해시·크기) ② 저장 서비스에 파일 저장 연결 ③ DB 실패 롤백 ④ 덮어쓰기 시 파일 교체·복원.
- **완료 기준**: 정상 저장 / 덮어쓰기 / 충돌 거절 / DB 실패 롤백 통합 테스트 통과. 저장된 파일의 SHA-256이 DB 기록과 일치.

## 단위 1-5 — DIMSE 서버 기반 + C-ECHO

- **무엇을**: dcm4che-net `Device`·`ApplicationEntity`·`Connection`을 Spring Bean으로 구성하고 앱 생명주기에 맞춰 시작·종료. AE Title·포트·호출 AE 허용 목록을 `application.yml` 설정으로. Verification SCP.
- **왜**: 이후 모든 DIMSE 서비스가 올라갈 기반. 설정과 생명주기를 먼저 고정한다.
- **학습 포인트**: DICOM 네트워크 모델(AE·association·presentation context·A-ASSOCIATE 협상), SCU/SCP 역할, dcm4che-net 구조(`Device`·`ApplicationEntity`·`Connection`), Spring Bean 생명주기와 `@ConfigurationProperties`.
- **진행 순서**: ① 테스트용 SCU로 C-ECHO 성공 테스트 ② 허용 목록 밖 AE Title 거절 테스트 ③ 비압축 외 전송구문 거절 테스트.
- **완료 기준**: 통합 테스트 통과, 로컬에서 `echoscu -c DCMIND@localhost:11112` 성공.

## 단위 1-6 — C-STORE SCP

- **무엇을**: Storage SCP. 수신 데이터셋 → `DicomParser` → 저장 서비스 → DIMSE 상태 응답. 수락 Storage SOP Class 목록 확정.
- **왜**: 모달리티가 PACS로 영상을 보내는 표준 경로.
- **학습 포인트**: Storage SOP Class, DIMSE 상태 코드 체계(Success·Warning·Failure), 수신 핸들러에서 도메인 서비스로의 경계, 한 association 안의 여러 C-STORE.
- **진행 순서**: ① 단일 인스턴스 저장 성공 ② 다중 인스턴스(한 association) ③ 거절 케이스 — 필수 태그 누락, 중복 SOP 충돌, 저장 실패. 각 상태 코드 값은 시작 시 PS3.4 Annex B·PS3.7 Annex C로 확정.
- **완료 기준**: 통합 테스트 통과, `storescu`로 `test-data/` 스터디 전송 → DB·스토리지 반영 확인.

## 단위 1-7 — C-FIND SCP

- **무엇을**: Study Root Query/Retrieve – FIND. STUDY·SERIES·IMAGE 레벨, 단일 값·와일드카드·날짜 범위·UID 목록 매칭, C-CANCEL 처리. 반환 속성 목록 확정.
- **왜**: 저장한 것을 찾을 수 있어야 PACS다. Phase 2 클라이언트 검색 화면이 이 서비스를 사용한다.
- **학습 포인트**: Query/Retrieve 정보 모델(Study Root)과 조회 레벨, Identifier 데이터셋, DICOM 매칭 규칙(단일 값·와일드카드·범위·UID 목록), JPA 동적 쿼리(Specification 등), 스트리밍 응답과 C-CANCEL.
- **진행 순서**: ① STUDY 레벨 PatientID 검색 ② 와일드카드·날짜 범위 ③ SERIES·IMAGE 레벨 ④ 0건·C-CANCEL.
- **완료 기준**: 통합 테스트 통과, `findscu`로 저장한 스터디 검색.

## 단위 1-8 — C-GET SCP

- **무엇을**: Study Root Query/Retrieve – GET. STUDY·SERIES·IMAGE 레벨 회수, 같은 association 위 C-STORE 하위 작업, Pending 응답의 하위 작업 카운트, 일부 실패 처리.
- **왜**: Phase 2 클라이언트 뷰어의 데이터 공급선.
- **학습 포인트**: C-GET과 C-MOVE의 차이, SCP/SCU Role Selection, 하위 작업(sub-operation)과 Pending 카운트, 파일 스트리밍 전송, 부분 실패 보고.
- **진행 순서**: ① IMAGE 레벨 단일 회수 ② SERIES·STUDY 레벨 ③ 하위 작업 실패·파일 누락 케이스.
- **완료 기준**: **Phase 1 완료 데모** — C-STORE로 보낸 파일이 C-FIND로 검색되고 C-GET으로 SHA-256이 같은 파일로 회수된다. 전 과정 통합 테스트 통과.

---

## 완료 기준 (스펙 §1과 동일)

1. C-ECHO 성공  2. C-STORE → 파싱·저장  3. C-FIND 검색  4. C-GET 회수(해시 일치)  5. 전 과정 자동 테스트
