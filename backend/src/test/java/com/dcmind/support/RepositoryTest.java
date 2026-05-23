package com.dcmind.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 리포지토리 테스트에서 반복적으로 쓰이는 어노테이션 패턴을 합성 어노테이션으로 만들어 보일러플레이트를 방지.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@Import(TestcontainersConfiguration.class) // TestcontainersConfiguration를 import하여 테스트 컨테이너 설정을 적용
@Transactional // 테스트를 트랜잭션으로 감싼다.(LazyInit(Patient) 시 세션이 닫힌 후 접근을 하여 테스트를 실패하는 현상 방지)
public @interface RepositoryTest { }
