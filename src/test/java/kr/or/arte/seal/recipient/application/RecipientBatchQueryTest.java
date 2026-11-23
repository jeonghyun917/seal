package kr.or.arte.seal.recipient.application;

import kr.or.arte.seal.recipient.crypto.*;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecipientBatchQueryTest {
    @Test void batchIsMaskedAndDoesNotFetchEachRecipientAgain() {
        var repository=mock(RecipientRepository.class);
        var cipher=mock(PersonalDataCipher.class);
        UUID actor=UUID.randomUUID(), id=UUID.randomUUID();
        var row=new RecipientStoredRecord(id,UUID.randomUUID(),new byte[30],new byte[30],"v1","a".repeat(64),"등록 근거",RecipientStatus.ACTIVE,0);
        when(repository.findManyScoped(eq(actor),anyList())).thenReturn(List.of(row));
        when(cipher.decrypt(any(),any())).thenAnswer(a->((PersonalDataContext)a.getArgument(0)).field().equals("name")?"홍길동":"hong@example.com");
        var result=new RecipientQueryService(repository,cipher).findMany(actor,List.of(id,id));
        assertThat(result).containsOnlyKeys(id);
        assertThat(result.get(id).maskedName()).isEqualTo("홍*동");
        assertThat(result.get(id).maskedEmail()).isNotEqualTo("hong@example.com");
        verify(repository).findManyScoped(actor,List.of(id));
        verify(repository,never()).findScoped(any(),any());
    }
    @Test void emptyBatchDoesNotReadTheDatabase() {
        var repository=mock(RecipientRepository.class);
        assertThat(new RecipientQueryService(repository,mock(PersonalDataCipher.class)).findMany(UUID.randomUUID(),List.of())).isEmpty();
        verifyNoInteractions(repository);
    }
}
