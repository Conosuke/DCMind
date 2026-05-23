package com.dcmind.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 시리즈 정보 Repository
// JpaRepository를 상속받아 기본적인 CRUD 기능을 제공하며, Series Instance UID로 스터디 정보를 조회하는 메서드를 정의
public interface SeriesRepository  extends JpaRepository<Series, Long>{

    // series instancs UID로 시리즈 정보를 조회
    Optional<Series> findBySeriesInstanceUid(String seriesInstanceUid);
}
