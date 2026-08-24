package kr.or.arte.seal.evidence.application;

import kr.or.arte.seal.evidence.domain.EvidenceGenerationStatus;
import kr.or.arte.seal.evidence.infrastructure.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;import org.springframework.web.server.ResponseStatusException;import org.springframework.http.HttpStatus;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.zip.*;

@Service
public class EvidenceQueryService {
    private final EvidenceRepository repository;private final LocalEvidenceStorage storage;private final String keyFile;private final Clock clock;
    public EvidenceQueryService(EvidenceRepository repository,@Value("${seal.evidence.root}")String root,
            @Value("${seal.evidence.hmac-key-file:}")String keyFile,Clock clock){this.repository=repository;this.storage=new LocalEvidenceStorage(Path.of(root));this.keyFile=keyFile;this.clock=clock;}
    public List<EvidencePackageSummary> search(UUID actor,EvidenceGenerationStatus status){return repository.search(actor,status);}
    public EvidencePackageDetail require(UUID actor,UUID id){return repository.findScoped(actor,id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"증적을 찾을 수 없습니다."));}
    public void requeue(UUID actor,UUID id){repository.requeue(actor,id,clock.instant());}
    public EvidenceVerificationResult verify(UUID actor,UUID id){EvidencePackageDetail detail=require(actor,id);EvidenceVerificationResult result=verify(detail);repository.recordVerification(id,actor,result,clock.instant());return result;}
    public byte[] download(UUID actor,UUID id){EvidencePackageDetail detail=require(actor,id);EvidenceVerificationResult result=verify(detail);repository.recordVerification(id,actor,result,clock.instant());if(result!=EvidenceVerificationResult.VALID)throw new IllegalStateException("무결성 검증에 실패한 증적입니다.");
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(bytes)){for(var file:detail.files()){zip.putNextEntry(new ZipEntry(file.fileName()));zip.write(storage.read(file.relativePath()));zip.closeEntry();}}return bytes.toByteArray();}catch(Exception e){throw new IllegalStateException("증적 ZIP을 만들 수 없습니다.",e);}}
    private EvidenceVerificationResult verify(EvidencePackageDetail detail){if(keyFile==null||keyFile.isBlank())throw new IllegalStateException("증적 HMAC 키가 설정되지 않았습니다.");Map<String,byte[]> all=new HashMap<>();for(var file:detail.files())all.put(file.fileName(),storage.read(file.relativePath()));byte[] manifest=all.remove("manifest.json");all.remove("manifest.hmac");if(manifest==null)return EvidenceVerificationResult.MISSING_FILE;return new EvidencePackageVerifier(new EvidenceHmacSigner(Path.of(keyFile))).verify(manifest,detail.manifestHmac(),all);}
}
