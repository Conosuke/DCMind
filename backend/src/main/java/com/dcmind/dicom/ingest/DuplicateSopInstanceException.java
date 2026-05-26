package com.dcmind.dicom.ingest;

// 이미 저장된 SOP Instance UID가 다른 Study 또는 Series로 다시 들어왔을 때 던지는 예외(SAME_SERIES 정책의 충돌 거절).
public class DuplicateSopInstanceException extends RuntimeException {

    public DuplicateSopInstanceException(String sopInstanceUid) {
        super("Duplicate SOP Instance UID: " + sopInstanceUid);
    }
}
