package kr.or.arte.seal.recipient.importing;

import kr.or.arte.seal.recipient.application.RecipientStoredRecord;
import org.springframework.stereotype.Repository;
import java.time.Instant; import java.util.List; import java.util.Optional; import java.util.UUID;

@Repository
public class MyBatisRecipientImportRepository implements RecipientImportRepository {
    private final RecipientImportMapper mapper;
    public MyBatisRecipientImportRepository(RecipientImportMapper mapper){this.mapper=mapper;}
    public boolean canAccessOrganization(UUID a,UUID o){return mapper.canAccessOrganization(a,o);}
    public boolean activeEmailExists(UUID o,String h){return mapper.activeEmailExists(o,h);}
    public void insertBatch(RecipientImportBatchRecord b,List<RecipientImportRowRecord> rows){mapper.insertBatch(b);rows.forEach(r->mapper.insertRow(r));}
    public Optional<RecipientImportBatchRecord> findScoped(UUID a,UUID b){return mapper.findScoped(a,b);}
    public Optional<RecipientImportBatchRecord> findScopedForUpdate(UUID a,UUID b){return mapper.findScopedForUpdate(a,b);}
    public List<RecipientImportRowRecord> findRows(UUID b){return mapper.findRows(b);}
    public UUID insertRecipientFromImport(RecipientStoredRecord r,UUID a,Instant n){mapper.insertRecipientFromImport(r,a,n);return r.recipientId();}
    public void markImported(UUID b,int r,UUID id){mapper.markImported(b,r,id);}
    public void complete(UUID b,int c,Instant n){mapper.purgeRemaining(b);mapper.complete(b,c,n);}
    public int expireDue(Instant n,int l){return mapper.expireDue(n,l);}
}
