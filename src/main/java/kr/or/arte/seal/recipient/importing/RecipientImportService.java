package kr.or.arte.seal.recipient.importing;

import kr.or.arte.seal.audit.application.AuditEventCommand;
import kr.or.arte.seal.audit.application.AuditEventWriter;
import kr.or.arte.seal.recipient.application.RecipientStoredRecord;
import kr.or.arte.seal.recipient.crypto.*;
import kr.or.arte.seal.recipient.domain.RecipientStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class RecipientImportService {
    private final RecipientImportRepository repository; private final RecipientWorkbookParser parser;
    private final UploadMalwareScanner scanner; private final PersonalDataCipher cipher;
    private final EmailLookupHasher hasher; private final AuditEventWriter audit; private final Clock clock; private final int maxBytes;
    public RecipientImportService(RecipientImportRepository repository,RecipientWorkbookParser parser,
            UploadMalwareScanner scanner,PersonalDataCipher cipher,EmailLookupHasher hasher,
            AuditEventWriter audit,Clock clock,RecipientImportProperties properties){this.repository=repository;this.parser=parser;this.scanner=scanner;
        this.cipher=cipher;this.hasher=hasher;this.audit=audit;this.clock=clock;this.maxBytes=properties.maxBytes();}

    @Transactional
    public RecipientImportPreview validate(UUID actorId,UUID orgId,String basis,byte[] file){
        if(actorId==null||orgId==null||!repository.canAccessOrganization(actorId,orgId)) throw notFound();
        if(file==null||file.length<4||file.length>maxBytes) throw new IllegalArgumentException("XLSX 파일은 설정된 최대 크기 이하여야 합니다.");
        if(file[0]!='P'||file[1]!='K') throw new IllegalArgumentException("XLSX 파일만 업로드할 수 있습니다.");
        String cleanBasis=validBasis(basis); MalwareScanResult scan=scanner.scan(file);
        if(scan==MalwareScanResult.INFECTED) throw new IllegalArgumentException("악성코드가 탐지되어 파일을 처리하지 않았습니다.");
        ParsedRecipientWorkbook parsed=parser.parse(new ByteArrayInputStream(file)); UUID batchId=UUID.randomUUID();
        Set<String> seen=new HashSet<>(); List<RecipientImportRowRecord> rows=new ArrayList<>();
        int valid=0,duplicate=0,error=0;
        for(var source:parsed.rows()){
            if(!source.valid()){
                rows.add(new RecipientImportRowRecord(batchId,source.rowNumber(),null,null,null,null,
                        PersonalDataMasker.name(source.name()),safeMaskedEmail(source.normalizedEmail()),
                        RecipientImportRowStatus.ERROR,String.join(",",source.errors()),null)); error++; continue;
            }
            String hmac=hasher.hmac(source.normalizedEmail()); boolean dup=!seen.add(hmac)||repository.activeEmailExists(orgId,hmac);
            EncryptedValue name=cipher.encrypt(context(batchId,orgId,source.rowNumber(),"name"),source.name());
            EncryptedValue email=cipher.encrypt(context(batchId,orgId,source.rowNumber(),"email"),source.normalizedEmail());
            RecipientImportRowStatus status=dup?RecipientImportRowStatus.DUPLICATE:RecipientImportRowStatus.VALID;
            rows.add(new RecipientImportRowRecord(batchId,source.rowNumber(),name.bytes(),email.bytes(),name.keyId(),hmac,
                    PersonalDataMasker.name(source.name()),PersonalDataMasker.email(source.normalizedEmail()),status,
                    dup?"DUPLICATE_EMAIL":null,null)); if(dup)duplicate++;else valid++;
        }
        Instant now=clock.instant(); RecipientImportBatchRecord batch=new RecipientImportBatchRecord(batchId,orgId,
                sha256(file),scan,cleanBasis,RecipientImportStatus.VALIDATED,rows.size(),valid,duplicate,error,0,
                now.plus(Duration.ofHours(24)),actorId,now,null,0);
        repository.insertBatch(batch,rows); audit("RECIPIENT_IMPORT_VALIDATE",batch,actorId);
        return preview(batch,rows);
    }

    @Transactional
    public RecipientImportResult confirm(UUID actorId,UUID batchId){
        RecipientImportBatchRecord batch=repository.findScopedForUpdate(actorId,batchId).orElseThrow(RecipientImportService::notFound);
        if(batch.status()==RecipientImportStatus.IMPORTED)return new RecipientImportResult(batchId,batch.importedCount(),true);
        if(batch.status()!=RecipientImportStatus.VALIDATED||!batch.expiresAt().isAfter(clock.instant()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"반입 확인 가능 시간이 지났습니다.");
        int count=0;
        for(var row:repository.findRows(batchId)) if(row.status()==RecipientImportRowStatus.VALID){
            String name=decrypt(row,batch,"name",row.nameCiphertext()); String email=decrypt(row,batch,"email",row.emailCiphertext());
            UUID id=UUID.randomUUID(); EncryptedValue n=cipher.encrypt(new PersonalDataContext("RECIPIENT",id,batch.organizationUnitId(),"name"),name);
            EncryptedValue e=cipher.encrypt(new PersonalDataContext("RECIPIENT",id,batch.organizationUnitId(),"email"),email);
            var recipient=new RecipientStoredRecord(id,batch.organizationUnitId(),n.bytes(),e.bytes(),n.keyId(),row.emailHmac(),
                    batch.collectionBasis(),RecipientStatus.ACTIVE,0);
            repository.insertRecipientFromImport(recipient,actorId,clock.instant()); repository.markImported(batchId,row.rowNumber(),id); count++;
        }
        repository.complete(batchId,count,clock.instant()); audit("RECIPIENT_IMPORT_CONFIRM",batch,actorId);
        return new RecipientImportResult(batchId,count,false);
    }

    @Transactional(readOnly=true)
    public RecipientImportPreview preview(UUID actorId,UUID batchId){
        var batch=repository.findScoped(actorId,batchId).orElseThrow(RecipientImportService::notFound);
        return preview(batch,repository.findRows(batchId));
    }
    @Transactional public int expireDue(int limit){return repository.expireDue(clock.instant(),limit);}

    private String decrypt(RecipientImportRowRecord row,RecipientImportBatchRecord batch,String field,byte[] bytes){
        return cipher.decrypt(context(batch.batchId(),batch.organizationUnitId(),row.rowNumber(),field),new EncryptedValue(row.keyId(),bytes));
    }
    private void audit(String action,RecipientImportBatchRecord batch,UUID actor){audit.append(new AuditEventCommand(clock.instant(),
            "ADMIN",actor,action,"RECIPIENT_IMPORT",batch.batchId().toString(),"SUCCESS",null,UUID.randomUUID(),
            Map.of("organizationUnitId",batch.organizationUnitId().toString(),"totalCount",Integer.toString(batch.totalCount()))));}
    private static RecipientImportPreview preview(RecipientImportBatchRecord b,List<RecipientImportRowRecord> rows){return new RecipientImportPreview(
            b.batchId(),b.status(),b.malwareScanStatus(),b.totalCount(),b.validCount(),b.duplicateCount(),b.errorCount(),b.importedCount(),b.expiresAt(),
            rows.stream().map(r->new RecipientImportRowView(r.rowNumber(),r.maskedName(),r.maskedEmail(),r.status(),r.errorCodes())).toList());}
    private static PersonalDataContext context(UUID batch,UUID org,int row,String field){return new PersonalDataContext("RECIPIENT_IMPORT",UUID.nameUUIDFromBytes((batch+":"+row).getBytes(java.nio.charset.StandardCharsets.UTF_8)),org,field);}
    private static String validBasis(String value){if(value==null||value.strip().length()<5||value.strip().length()>500)throw new IllegalArgumentException("수집 근거는 5~500자로 입력해 주세요.");return value.strip();}
    private static String safeMaskedEmail(String value){try{return PersonalDataMasker.email(value);}catch(RuntimeException ignored){return "***";}}
    private static String sha256(byte[] value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));}catch(Exception e){throw new IllegalStateException(e);}}
    private static ResponseStatusException notFound(){return new ResponseStatusException(HttpStatus.NOT_FOUND);}
}
