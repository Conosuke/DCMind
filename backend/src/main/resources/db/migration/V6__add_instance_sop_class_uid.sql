/*
  instance에 SOP Class UID를 보관한다.

  SOP Class UID는 DICOM 객체 종류를 나타내므로, 이후 영상 객체와 Structured Report를
  같은 instance 테이블에서 구분하는 기준으로 사용한다.

  기존 instance 행에는 원본 DICOM의 SOP Class UID가 저장되어 있지 않다.
  임의의 객체 종류를 채우지 않기 위해 이 열은 nullable로 추가한다.
*/
ALTER TABLE instance ADD COLUMN sop_class_uid VARCHAR(64);
