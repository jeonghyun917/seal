package kr.or.arte.seal.evidence.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.or.arte.seal.evidence.infrastructure.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Component
public class EvidencePackageGenerator implements EvidenceArtifactGenerator {
    private final Path root; private final String keyFile; private final String activeKeyId; private final ObjectMapper json;
    private final EvidenceManifestFactory manifests=new EvidenceManifestFactory();
    private final OpenHtmlEvidencePdfRenderer pdf=new OpenHtmlEvidencePdfRenderer();
    public EvidencePackageGenerator(@Value("${seal.evidence.root}") String root,
            @Value("${seal.evidence.hmac-key-file:}") String keyFile,
            @Value("${seal.evidence.hmac-active-key-id:v1}") String activeKeyId,ObjectMapper json){
        this.root=Path.of(root).toAbsolutePath().normalize();this.keyFile=keyFile;this.activeKeyId=activeKeyId;this.json=json;
    }
    public GeneratedEvidence generate(EvidenceWorkItem item){
        if(keyFile==null||keyFile.isBlank())throw new IllegalStateException("증적 HMAC 키가 설정되지 않았습니다.");
        try{
            Map<String,byte[]> content=new LinkedHashMap<>();
            content.put("evidence.pdf",pdf.render(item.requestId(),item.title(),item.decisions()));
            content.put("request.json",item.snapshotJson().getBytes(StandardCharsets.UTF_8));
            content.put("decisions.json",json.writeValueAsBytes(item.decisions()));
            content.put("events.json",json.writeValueAsBytes(item.events()));
            EvidenceManifest manifest=manifests.create(content); EvidenceHmacSigner signer=new EvidenceHmacSigner(Path.of(keyFile),activeKeyId);
            String signature=signer.sign(manifest.canonicalJson());
            Map<String,byte[]> all=new LinkedHashMap<>(content);all.put("manifest.json",manifest.canonicalJson());all.put("manifest.hmac",signature.getBytes(StandardCharsets.US_ASCII));
            new LocalEvidenceStorage(root).write(item.organizationId(),item.requestId(),item.packageId(),all);
            String directory=item.organizationId()+"/"+item.requestId()+"/"+item.packageId();
            List<EvidenceFileMetadata> files=new ArrayList<>();
            Map<String,String> types=Map.of("evidence.pdf","PDF","request.json","REQUEST_JSON","decisions.json","DECISIONS_JSON","events.json","EVENTS_JSON","manifest.json","MANIFEST_JSON","manifest.hmac","MANIFEST_HMAC");
            all.forEach((name,bytes)->files.add(new EvidenceFileMetadata(types.get(name),name,directory+"/"+name,bytes.length,EvidenceManifestFactory.sha256(bytes))));
            return new GeneratedEvidence(directory,EvidenceManifestFactory.sha256(manifest.canonicalJson()),signature,signer.activeKeyId(),files);
        }catch(RuntimeException exception){throw exception;}catch(Exception exception){throw new IllegalStateException("증적 패키지를 만들 수 없습니다.",exception);}
    }
}
