# 6차 미니 프로젝트 - EKS 기반 도서 서비스 CI/CD 및 운영 자동화

## 1. 프로젝트 개요

본 프로젝트는 5차 미니프로젝트에서 구축한 Spring Boot 기반 도서관리시스템을 AWS 환경으로 이전하여, Kubernetes(EKS) 기반의 CI/CD 자동 배포 및 운영 모니터링 체계를 구축한 프로젝트이다.

GitHub에 코드를 Push하면 AWS CodePipeline이 자동으로 동작하여 Docker 이미지 빌드, ECR 저장, 운영자 수동 승인, EKS 배포까지 무중단으로 진행된다. 또한 CloudWatch를 통한 로그/지표/알람 모니터링과 HPA 기반 오토스케일링을 적용하여 트래픽 변화에 자동으로 대응할 수 있는 운영 환경을 구성하였다.

본 프로젝트의 핵심 목표는 다음과 같다.

- AWS 환경(EKS, ECR, CodePipeline, CodeBuild) 기반 인프라 구축
- Docker 기반 Frontend/Backend 컨테이너화
- GitHub 브랜치 전략(main/dev) 및 PR 기반 협업 도입
- CodePipeline을 통한 CI/CD 자동 배포 구현
- Manual Approval 단계 및 SNS 이메일 알림 연동
- EKS 클러스터에 Frontend/Backend 배포 및 LoadBalancer 외부 노출
- Rolling Update를 통한 무중단 배포 구현
- HPA(Horizontal Pod Autoscaler) 기반 오토스케일링 구현
- 기존 H2 file DB를 RDS(MySQL)로 전환
- CloudWatch Logs/Dashboard/Alarm을 통한 운영 모니터링 체계 구축
- 장애 상황(빌드 실패, 배포 실패, 파드 장애) 재현 및 복구 실습

## 2. 팀원 R&R

| 이름 | 역할 | 담당 내용 |
|------|------|-----------|
| 한준우 | 조장 / Dev | 프로젝트 총괄, 개발 진행 관리 |
| 이성민 | 발표자 / Dev | 발표 진행, 개발 |
| 박시우 | PPT 제작자 / Monitoring | 발표 자료 제작, CloudWatch 모니터링 구성 |
| 양경동 | PPT 제작자 / Dev | 발표 자료 제작, 개발 |
| 정휘재 | 서기 / Infra | 회의 기록, 인프라(EKS, 네트워크 등) 구성 |
| 서한석 | 검토담당자 / Monitoring | 결과물 검토, CloudWatch 모니터링 구성 |
| 최준석 | 타임키퍼 / Infra | 일정 관리, 인프라 구성 |

## 3. 전체 아키텍처

```
GitHub (main/dev)
    |
    | Push / Pull Request
    v
AWS CodePipeline
    |
    | Source
    v
AWS CodeBuild (Build)
    |
    | Docker Build
    v
Amazon ECR (이미지 저장)
    |
    | Manual Approval (SNS 이메일 알림)
    v
AWS CodeBuild (DeployToEKS)
    |
    | kubectl apply / rollout
    v
Amazon EKS Cluster
 ├─ frontend Pod / Service (LoadBalancer)
 └─ backend Pod / Service
    |
    v
Amazon RDS (MySQL)
```

모니터링 흐름은 다음과 같다.

```
CodeBuild / EKS
    |
    v
Amazon CloudWatch
 ├─ Logs (빌드/배포 로그 수집)
 ├─ Dashboard (CPU 등 지표 시각화)
 └─ Alarm (이상 감지)
        |
        v
   Amazon SNS → 이메일 알림
```

## 4. 기술 스택

| 구분 | 기술 |
|------|------|
| Frontend | React 19, Vite, JavaScript |
| Backend | Java 17, Spring Boot, Spring MVC, Spring Data JPA |
| Container | Docker |
| Container Orchestration | Amazon EKS, Kubernetes (Pod, Deployment, Service, HPA) |
| CI/CD | AWS CodePipeline, AWS CodeBuild |
| Image Registry | Amazon ECR |
| Database | Amazon RDS (MySQL) |
| 알림 | Amazon SNS |
| 모니터링 | Amazon CloudWatch (Logs, Dashboard, Alarm) |
| 네트워크 / 노출 | LoadBalancer (Kubernetes Service) |
| 인증 정보 관리 | Kubernetes Secret |
| 협업 | GitHub (main/dev 브랜치 전략, Pull Request) |
| 개발 환경 | IntelliJ IDEA, VS Code |

## 5. CI/CD 파이프라인 구성

### 5.1 브랜치 전략 (main/dev)

1. main/dev 브랜치 생성
2. 개발 내용 Push 및 Pull Request 생성 (팀장에게 수정 요청)
3. 코드 리뷰 후 Merge

### 5.2 파이프라인 단계

| 단계 | 설명 |
|------|------|
| Source | GitHub 소스 변경 감지 |
| Build | Dockerfile 기반 Frontend/Backend 이미지 빌드 후 ECR 저장 |
| ManualApproval | 운영자 승인 단계 (SNS 이메일 알림) |
| DeployToEKS | CodeBuild에서 kubectl apply / rollout 명령 수행하여 EKS에 배포 |
| Deploy | 산출물 보관 |

운영자 승인 한 번만 거치면 전체 배포가 자동화되어, 사람의 개입을 최소화한 안정적인 배포 체계를 구축하였다.

## 6. Kubernetes 리소스 구성

- **frontend / backend Pod**: 각각 Deployment로 관리
- **frontend-service**: LoadBalancer 타입으로 외부 접속 제공
- **backend-service**: 내부 통신용 Service
- **HPA(backend)**: 최소 2개 ~ 최대 10개, CPU 목표 사용률 60%
- **metrics-server**: CPU 사용량 측정을 위한 EKS 관리형 애드온
- **Secret**: RDS 접속 정보(DB 계정, 비밀번호 등)를 backend Pod에 주입

## 7. Auto Scaling / Rolling Update

### 7.1 오토스케일링 (HPA)

- backend 최소 2개 ~ 최대 10개, CPU 목표 60%로 HPA 구성
- metrics-server 연동으로 CPU 사용량 측정
- 부하 테스트로 CPU 사용량을 높이자 파드가 2개 → 10개까지 자동 증설되었고, 부하가 사라지면 다시 최소 2개로 축소됨을 확인

### 7.2 Rolling Update (무중단 배포)

- 백엔드를 새 버전으로 배포하여 무중단 업데이트 동작을 확인
- 기존 버전 파드가 새 버전으로 하나씩 순차 교체되었고, 서비스 중단 없이 배포가 완료됨

## 8. 데이터베이스 전환 (H2 → RDS)

기존 5차 프로젝트에서 사용하던 H2 file DB는 Pod 재시작 시 데이터가 유지되지 않는 한계가 있어, RDS(MySQL)로 전환하였다. DB 접속 정보는 Kubernetes Secret을 통해 backend Pod에 주입하여 보안성을 확보하였다.

## 9. 모니터링 및 알림 (CloudWatch / SNS)

CloudWatch를 통해 운영 중인 서비스를 다음 세 가지 방식으로 모니터링한다.

| 구성 요소 | 설명 |
|-----------|------|
| Logs | 빌드·배포 로그 자동 수집 (`02_book_build`, `02_book_deploy`) |
| Dashboard | CPU 사용량 등 주요 지표를 그래프와 테이블로 시각화 |
| Alarm | CodeBuild 빌드 실패, 배포 실패, ELB 비정상 대상 수, 파드 무한 재시작, 메모리 한계 도달, HTTP 에러 등 이상 상황 감지 |

CloudWatch Alarm이 발생하면 SNS Topic을 통해 운영자에게 이메일 알림이 전송되도록 구성하였으며, `codebuild-failed-alarm`이 실제 빌드 실패 시 정상적으로 SNS 메일을 전송함을 확인하였다.

## 10. 실행 방법

### 10.1 사전 준비

- AWS 계정 및 EKS, ECR, RDS, CodePipeline, CodeBuild, CloudWatch, SNS 리소스 구성
- `kubectl`, `aws-cli`가 설치되고 EKS 클러스터에 접근 가능한 환경

### 10.2 Backend / Frontend 이미지 빌드 (로컬 테스트용)

```bash
# Backend
cd backend
docker build -t book-backend .

# Frontend
cd frontend
docker build -t book-frontend .
```

### 10.3 CI/CD 배포 (운영 환경)

1. dev 브랜치에서 작업 후 Pull Request 생성
2. 코드 리뷰 후 main 브랜치로 Merge
3. GitHub Push 감지 시 CodePipeline 자동 실행
4. CodeBuild에서 Docker 이미지 빌드 후 ECR에 저장
5. ManualApproval 단계에서 운영자가 SNS 이메일 확인 후 승인
6. DeployToEKS 단계에서 kubectl apply / rollout 수행
7. EKS 클러스터에 frontend/backend Pod 배포 완료

### 10.4 서비스 접속

```bash
kubectl get service frontend-service
```

위 명령으로 확인한 LoadBalancer 외부 주소(DNS)로 접속한다.

## 11. 시연 테스트 시나리오

### 11.1 CI/CD 배포 테스트

| 순서 | 테스트 항목 | 확인 내용 |
|------|-------------|-----------|
| 1 | 코드 Push | GitHub Push 시 CodePipeline이 자동으로 실행되는지 확인 |
| 2 | Build | CodeBuild에서 Docker 이미지가 정상적으로 빌드되고 ECR에 저장되는지 확인 |
| 3 | Manual Approval | 운영자에게 SNS 이메일 알림이 전송되고, 승인 후 다음 단계로 진행되는지 확인 |
| 4 | DeployToEKS | kubectl apply/rollout 명령이 성공적으로 수행되어 EKS에 배포되는지 확인 |
| 5 | 서비스 접속 | LoadBalancer 주소로 외부에서 정상 접속되는지 확인 |

### 11.2 Auto Scaling 테스트

| 순서 | 테스트 항목 | 확인 내용 |
|------|-------------|-----------|
| 1 | 부하 발생 | 부하 테스트 도구로 backend에 트래픽을 증가시킴 |
| 2 | 파드 증설 확인 | CPU 사용량 증가에 따라 파드가 2개에서 최대 10개까지 자동 증설되는지 확인 |
| 3 | 파드 축소 확인 | 부하 종료 후 파드가 다시 최소 2개로 축소되는지 확인 |

### 11.3 Rolling Update 테스트

| 순서 | 테스트 항목 | 확인 내용 |
|------|-------------|-----------|
| 1 | 신규 버전 배포 | backend 신규 버전 배포 시 기존 파드가 순차적으로 교체되는지 확인 |
| 2 | 무중단 확인 | 배포 중 서비스 중단 없이 정상 응답하는지 확인 |

### 11.4 장애 대응 테스트

| 순서 | 테스트 항목 | 확인 내용 |
|------|-------------|-----------|
| 1 | 빌드 실패 재현 | Node 버전 호환성 문제로 빌드가 실패하는 상황을 재현하고 원인 분석 |
| 2 | 배포 실패 및 롤백 | 잘못된 이미지로 배포 시 ErrImagePull이 발생해도 기존 파드가 유지되는지 확인 후 롤백 수행 |
| 3 | 파드 강제 삭제 (Auto Healing) | 파드를 강제 삭제했을 때 Kubernetes가 자동으로 새 파드를 생성하여 복구하는지 확인 |
| 4 | CloudWatch Alarm 확인 | 장애 발생 시 CloudWatch Alarm이 동작하고 SNS 이메일이 전송되는지 확인 |

## 12. 트러블슈팅

| 문제 상황 | 원인 | 해결 방법 |
|-----------|------|-----------|
| 오토스케일링 시 CPU 메트릭이 수집되지 않음 | 2개 노드가 SchedulingDisabled 상태로 파드가 한 노드에 집중되어 IP 고갈 발생 | `uncordon` 명령으로 노드 스케줄링 해제 |
| metrics-server 정상 동작 안 함 | aws-node(CNI) 오류로 파드가 IP를 할당받지 못함 | aws-node 재시작으로 복구 |
| metrics-server 메트릭 전달 실패 | EKS 관리형 애드온에 수동 설정이 충돌(UnsupportedAddonModification) | 수동 설정 제거 후 애드온 재설치 |
| 프론트엔드 빌드 실패 (`CustomEvent is not defined`) | 빌드 도구(Vite)가 Node 20 이상을 요구하나 Node 18 사용 | Dockerfile의 Node 버전을 18에서 20으로 변경 |
| 배포 실패 (`ErrImagePull`) | 잘못된 이미지 태그로 배포 시도 | 기존 파드는 유지되어 서비스 중단 없이 롤백 명령으로 즉시 복구 |
| H2 DB 데이터 유실 | Pod 재시작 시 H2 file DB 데이터가 유지되지 않음 | RDS(MySQL)로 전환하고 Kubernetes Secret으로 접속 정보 주입 |
| 파드 장애 시 서비스 중단 우려 | 파드 강제 종료/장애 상황 | Kubernetes의 자동 복구(Auto Healing)로 약 2초 내 새 파드 생성 및 복구 |

## 13. 주요 구현 결과

### 13.1 인프라 자동화 (CI/CD)

GitHub Push만으로 빌드부터 배포까지 자동으로 진행되는 CodePipeline을 구축하였다. ManualApproval 단계를 추가하여 운영자가 최종 배포 여부를 결정할 수 있도록 하였으며, SNS 이메일 알림을 통해 승인 요청을 즉시 인지할 수 있도록 구성하였다.

### 13.2 EKS 기반 배포

Frontend/Backend를 각각 Docker 이미지로 빌드하여 ECR에 저장하고, EKS 클러스터에 Pod/Service로 배포하였다. frontend-service는 LoadBalancer 타입으로 외부에 노출하여 React 기반 도서 서비스에 접속할 수 있도록 구성하였다.

### 13.3 무중단 배포 및 오토스케일링

Rolling Update를 통해 신규 버전 배포 시에도 서비스 중단이 발생하지 않음을 확인하였다. 또한 HPA를 통해 backend Pod가 트래픽에 따라 2개에서 최대 10개까지 자동으로 증설/축소되는 것을 부하 테스트로 실증하였다.

### 13.4 데이터베이스 전환

기존 H2 file DB의 데이터 유지 한계를 해결하기 위해 RDS(MySQL)로 전환하였으며, DB 접속 정보는 Kubernetes Secret으로 안전하게 관리하였다.

### 13.5 모니터링 및 알림 체계 구축

CloudWatch Logs, Dashboard, Alarm을 통합 구성하여 빌드/배포 로그 수집, 지표 시각화, 이상 상황 감지를 실시간으로 수행할 수 있도록 하였다. 이상 상황 발생 시 SNS를 통해 이메일로 즉시 알림을 받을 수 있도록 구성하여, 사람의 개입을 최소화한 안정적인 운영 환경을 구축하였다.

### 13.6 장애 대응 체계 검증

빌드 실패, 배포 실패, 파드 장애 상황을 직접 재현하여 각 상황에서의 원인 분석과 복구 절차(버전 수정, 롤백, Auto Healing)를 검증하였다.

## 14. 프로젝트 의의

본 프로젝트를 통해 5차 미니프로젝트에서 구현한 Spring Boot 기반 도서관리시스템을 실제 클라우드 운영 환경으로 확장하는 경험을 수행하였다. AWS EKS, CodePipeline, CodeBuild, ECR, RDS, CloudWatch, SNS 등 다양한 AWS 서비스를 연계하여 CI/CD 자동화와 운영 모니터링 체계를 직접 구축하였으며, 오토스케일링과 Rolling Update를 통해 트래픽 변화와 배포 상황에도 안정적으로 동작하는 서비스를 구현하였다.

또한 오토스케일링 메트릭 수집 실패, 빌드 실패, 배포 실패 등 실제 장애 상황을 재현하고 단계적으로 원인을 추적하여 해결함으로써, 운영 환경에서 발생할 수 있는 문제에 대한 트러블슈팅 역량을 강화하였다.
