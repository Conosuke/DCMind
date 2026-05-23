package com.dcmind.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/*
    * TestcontainersConfiguration 클래스는 테스트 환경에서 PostgreSQL 데이터베이스 컨테이너를 설정하는 역할을 함.
*/
@TestConfiguration(proxyBeanMethods = false) // Spring Boot TestConfiguration 어노테이션을 사용하여 테스트 환경에서만 적용되는 설정임을 명시
public class TestcontainersConfiguration {

    @Bean // PostgreSQLContainer를 Bean으로 등록하여 테스트 환경에서 사용할 수 있도록 함.
    @ServiceConnection // 컨테이너의 접속 정보를 Spring datasource에 자동 주입(application.yaml 설정 대신)
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:16"); // PostgreSQL 16 버전의 컨테이너를 생성
    }
}
