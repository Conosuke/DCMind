# DCMind — 프로젝트 개요

AI 에이전트를 갖춘 PACS(Picture Archiving and Communication System). WPF 데스크톱 클라이언트와 PACS 서버로 구성한다.
포트폴리오 프로젝트이며, 아키텍처는 실사용 확장이 가능하도록 진지하게 설계한다. 최우선 목적은 **학습**.

> 작업 규칙은 루트의 `CLAUDE.md` 참고(로컬 전용, git 미추적). 이 문서와 `docs/`는 **프로젝트 내용**(개요·결정·로드맵·스펙)을 담는다.

---

## 확정된 기술 결정

| 항목 | 결정 |
|---|---|
| 목적 | 포트폴리오 (실사용 확장 열어둠), 학습 우선 |
| 진행 방식 | 에이전트가 구현·설명, 사용자는 설계 결정 참여·코드 리뷰·이해도 점검으로 학습 |
| 시스템 구성 | **PACS 서버 + WPF 데스크톱 클라이언트** |
| 서버 | **Java / Spring Boot** — 한 프로세스가 DIMSE 포트와 HTTP 포트를 함께 연다 |
| DICOM 파싱 | **dcm4che** 라이브러리 사용, 그 위 계층(저장 구조·인덱싱·조회)을 직접 설계·구현 |
| DICOM 네트워크 | **DIMSE** (dcm4che-net) — C-ECHO, C-STORE, C-FIND, C-GET SCP |
| 영상 수신 | 모달리티 → 서버 **C-STORE** |
| 클라이언트 영상 조회 | **C-FIND**(검색) + **C-GET**(가져오기). 한 association 안에서 받으므로 클라이언트가 별도 Storage SCP를 열 필요가 없다 |
| 비영상 기능 | **HTTP REST API** — 로그인·권한, 판독 워크플로우, 리포트, 관리, AI 에이전트 |
| 전송구문(Transfer Syntax) | **비압축만** 우선 (Implicit/Explicit VR Little Endian). association 협상 단계에서 비압축 전송구문만 수락한다. 압축은 이후 Phase |
| 클라이언트 | **C# / WPF (.NET, MVVM)**. DICOM 통신·렌더링은 **fo-dicom**. 정확한 버전은 Phase 2 시작 시 공식 문서로 확정 |
| 디자인 | `docs/design/guide.md` 토큰을 XAML `ResourceDictionary`로 구현, 레이아웃은 `prototype/*.html` 기준 |
| 병원 플로우 | **EMR Mock + Modality Mock** 으로 전체 흐름 재현 |
| 시스템 포지션 | **RIS/PACS 통합형** — 독립형 RIS를 두지 않고, RIS의 워크플로우 기능을 DCMind가 흡수 (현대 통합형 제품 모델, 예: Fujifilm Synapse 계열) |
| EMR ↔ PACS 상호운용 | **HL7 FHIR** (ServiceRequest=오더 / DiagnosticReport=결과) |
| 모달리티 워크리스트 | **DICOM Modality Worklist (MWL, C-FIND SCP)** |
| AI | **대화형 LLM 에이전트 + tool calling** (권장: Claude). Vision AI(영상판독)는 미래 옵션. LLM 프로바이더 최종 확정은 Phase 4에서 |

---

## 전체 시스템 흐름 (목표)

```
[EMR Mock] ──FHIR ServiceRequest(오더)──▶ [PACS 서버] 오더·워크리스트
                                              │
[Modality Mock] ──MWL C-FIND(워크리스트 조회)──┘
      │  DICOM 생성/로드 → C-STORE 전송
      ▼
[PACS 서버] ← dcm4che로 파싱 → 저장/인덱싱 (저장 구조·조회는 직접 설계)
      │
      ├──DIMSE (C-FIND / C-GET)──▶ [WPF 클라이언트] 워크리스트·뷰어 — 판독의가 조회·측정
      ├──HTTP REST──────────────▶ [WPF 클라이언트] 로그인·판독 상태·리포트·에이전트 챗
      ├──▶ [LLM 에이전트]  자연어로 조회·리포트 초안·워크플로우 조작
      │
      └──FHIR DiagnosticReport(결과)──▶ [EMR Mock]
```

### 서버 포트 구성

| 포트 | 프로토콜 | 용도 |
|---|---|---|
| 11112 | DICOM (DIMSE) | C-ECHO / C-STORE / C-FIND / C-GET, MWL |
| 8080 | HTTP | REST API, FHIR 엔드포인트 |

- 서버 AE Title 기본값은 `DCMIND`. 호출 AE Title 허용 목록으로 DIMSE 접근을 제한한다.

---

## 로드맵 (Phase)

각 Phase는 독립적으로 데모 가능하며, 각자 스펙 → 구현 사이클을 가진다. Phase별 설계 스펙은 `docs/specs/`, 구현 계획은 `docs/plans/`에 둔다.

- **Phase 0 — 기반**: 프로젝트 스캐폴드, 저장소(메타데이터=Postgres, DICOM 원본=파일/오브젝트 스토리지), Docker Compose, CI.
- **Phase 1 — DICOM 코어 + DIMSE 서버 ⭐**: dcm4che 기반 파싱 계층, Patient/Study/Series/Instance 저장·인덱싱, 원본 파일 저장·SHA-256, C-ECHO / C-STORE / C-FIND / C-GET SCP. 데모는 dcm4che 명령행 도구(`storescu`·`findscu`·`getscu`)로 한다.
- **Phase 2 — WPF 클라이언트 (검색 + 뷰어)**: 디자인 토큰 `ResourceDictionary`, 스터디 검색(C-FIND), 영상 가져오기(C-GET), DICOM 렌더링, window/level·확대·이동·기본 측정, 로컬 캐시.
- **Phase 3 — 병원 플로우 / RIS 기능 계층 + REST API + Mock 액터**: 인증(JWT·역할)과 REST API 기반, DCMind가 흡수하는 RIS 워크플로우 기능 — 오더 접수(FHIR ServiceRequest→Accession Number 발급→워크리스트), MWL C-FIND SCP, 검사 상태 추적(오더됨→촬영완료→판독대기→판독완료), 판독 리포트 작성·관리, 리포트 EMR 회신(FHIR DiagnosticReport), 클라이언트 워크리스트·리포트 화면. Mock 액터 — Modality Mock(MWL 조회→샘플 DICOM→C-STORE), EMR Mock(오더 발행·리포트 수신).
  - *범위 제외 (RIS 기능 중 비채택)*: 검사실 예약·스케줄링 최적화, 수가 청구·수납, 부서 통계·행정.
  - *스토리지 운영 기능*: 용량·통계 조회 API, 무결성 검증 잡(저장 시 기록한 SHA-256 기준), 고아 데이터 탐지, 클라이언트 관리 화면.
- **Phase 4 — LLM 에이전트**: 서버 기능을 도구로 호출(검색·열기·과거검사 요약·리포트 초안·EMR 회신), WPF 클라이언트에 챗 패널 통합.
- **Phase 5+ (미래)**: 압축 전송구문, C-MOVE(외부 노드 전송), DICOMweb, DICOM TLS, Vision AI 연동, 인증 강화, 스토리지 라이프사이클(보존 정책·계층화·백업).

---

## 리포지토리 폴더 구조 (Phase 진행에 따라 확장)

모노레포로 구성한다. `backend/`(PACS 서버)가 먼저 존재하고, Phase가 진행되며 `client/`, `mocks/`가 추가된다.

```
DCMind/
├─ CLAUDE.md                  # 작업 규칙 (로컬 전용, git 미추적)
├─ LICENSE
├─ .gitignore                 # Java/Gradle + .NET + 에이전트 로컬 파일
├─ docker-compose.yml         # Postgres 등 인프라
├─ README.md                  # 프로젝트 소개 (포트폴리오용)
│
├─ docs/                      # ── 프로젝트 문서 ──
│  ├─ overview/               # 개요·결정·로드맵·구조 (이 문서)
│  ├─ design/                 # 디자인 가이드 (토큰·컴포넌트·원칙)
│  ├─ erd/                    # 데이터 모델 다이어그램
│  ├─ specs/                  # Phase별 설계 스펙 (예: phase-1-dicom-core.md)
│  └─ plans/                  # Phase별 구현 계획 (작업 단위·학습 포인트)
│
├─ prototype/                 # 화면 레이아웃 기준 (worklist·viewer·admin HTML 목업)
│
├─ backend/                   # ── PACS 서버 (Spring Boot) ──
│  ├─ build.gradle
│  ├─ settings.gradle
│  └─ src/
│     ├─ main/
│     │  ├─ java/com/dcmind/
│     │  │  ├─ dicom/         # DICOM 계층 (dcm4che 래핑)
│     │  │  │  ├─ parse/      # dcm4che로 파일 읽기·태그 추출·검증
│     │  │  │  ├─ model/      # 파싱 결과 도메인 객체 (dcm4che 타입을 밖으로 노출하지 않음)
│     │  │  │  └─ ingest/     # 파싱 결과 저장 오케스트레이션
│     │  │  ├─ dimse/         # DIMSE SCP (C-ECHO/C-STORE/C-FIND/C-GET, MWL)
│     │  │  ├─ store/         # 저장·인덱싱 (Patient/Study/Series/Instance)
│     │  │  ├─ api/           # REST API 컨트롤러 (Phase 3)
│     │  │  ├─ fhir/          # FHIR 파사드 (Phase 3)
│     │  │  ├─ worklist/      # 오더·워크리스트 (Phase 3)
│     │  │  ├─ agent/         # LLM 에이전트 오케스트레이션 (Phase 4)
│     │  │  └─ auth/          # 인증·권한 (Phase 3)
│     │  └─ resources/        # application.yml, DB 마이그레이션 등
│     └─ test/                # 유닛·통합 테스트
│
├─ client/                    # ── WPF 클라이언트 (.NET) ── (Phase 2~)
│  ├─ DCMind.Client.sln
│  ├─ src/
│  │  ├─ DCMind.Client/       # WPF 앱 (Views, ViewModels, Themes/ResourceDictionary)
│  │  └─ DCMind.Client.Core/  # UI 비의존 로직 (DIMSE·REST 클라이언트, 도메인 모델)
│  └─ tests/
│     └─ DCMind.Client.Core.Tests/
│
└─ mocks/                     # ── Mock 액터 ── (Phase 3)
   ├─ modality-mock/          # MWL 조회 → DICOM 생성 → C-STORE 전송
   └─ emr-mock/               # FHIR 오더 발행 · 리포트 수신
```

> 참고: `mocks/`의 구현 언어와 클라이언트 테스트 프레임워크는 각 Phase 시작 시점에 확정한다.
