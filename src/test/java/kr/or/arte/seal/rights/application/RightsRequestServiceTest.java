package kr.or.arte.seal.rights.application;

import kr.or.arte.seal.recipient.crypto.EncryptedValue;
import kr.or.arte.seal.recipient.crypto.PersonalDataCipher;
import kr.or.arte.seal.retention.application.RetentionJobRequestPort;
import kr.or.arte.seal.rights.domain.RightsRequestStatus;
import kr.or.arte.seal.rights.domain.RightsRequestType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RightsRequestServiceTest {
    @Test
    void completingRejectedErasureOnlyRecordsNotificationAndDoesNotCreateDestructionJob() {
        RightsRequestRepository repository = mock(RightsRequestRepository.class);
        PersonalDataCipher cipher = mock(PersonalDataCipher.class);
        RetentionJobRequestPort retention = mock(RetentionJobRequestPort.class);
        Instant now = Instant.parse("2026-08-24T06:00:00Z");
        UUID actor = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        UUID org = UUID.randomUUID();
        RightsRequestRecord record = new RightsRequestRecord(id, "RR-TEST", org, UUID.randomUUID(),
                UUID.randomUUID(), RightsRequestType.ERASURE, RightsRequestStatus.REJECTED,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), new byte[]{1}, null, null,
                "key-1", "OFFLINE", UUID.randomUUID(), now.minusSeconds(60), UUID.randomUUID(), null, 4);
        when(repository.findScopedForUpdate(actor, id)).thenReturn(Optional.of(record));
        when(cipher.encrypt(any(), eq("반려 사유 통지 완료"))).thenReturn(new EncryptedValue("key-1", new byte[]{2}));
        RightsRequestService service = new RightsRequestService(repository, cipher, retention,
                Clock.fixed(now, ZoneOffset.UTC));

        service.complete(actor, id, "반려 사유 통지 완료");

        verifyNoInteractions(retention);
        verify(repository).transition(eq(id), eq(RightsRequestStatus.REJECTED),
                eq(RightsRequestStatus.COMPLETED), eq(actor), eq(null), eq(null), eq("key-1"),
                eq(null), any(byte[].class), eq(now), eq(4L));
    }
}
