package kr.or.arte.seal.template.application;

import kr.or.arte.seal.template.persistence.ConsentTemplatePageRow;
import kr.or.arte.seal.template.persistence.ConsentTemplateQueryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConsentTemplateQueryService {
    private final ConsentTemplateQueryMapper mapper;
    private final ConsentTemplateRepository repository;

    public ConsentTemplateQueryService(ConsentTemplateQueryMapper mapper,
                                       ConsentTemplateRepository repository) {
        this.mapper = mapper;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ConsentTemplateSummary> search(UUID actorId, String keyword, String status) {
        return mapper.search(actorId, normalize(keyword), normalize(status));
    }

    @Transactional(readOnly = true)
    public List<DeletedConsentTemplateSummary> searchDeleted(UUID actorId, String keyword) {
        return mapper.searchDeleted(actorId, normalize(keyword));
    }

    @Transactional(readOnly = true)
    public ConsentTemplatePage requireScoped(UUID templateId, UUID actorId) {
        List<ConsentTemplatePageRow> rows = mapper.findPage(actorId, templateId);
        if (rows.size() != 1) {
            throw new TemplateNotFoundException();
        }
        ConsentTemplatePageRow row = rows.get(0);
        return new ConsentTemplatePage(row.consentTemplateId(), row.templateCode(),
                row.templateName(), row.processingActivityId(), row.processingActivityName(),
                row.templateStatus(), row.currentPublishedVersionId(), row.versionNo(),
                mapper.findVersions(actorId, templateId));
    }

    public ConsentTemplateDetail requireVersionScoped(UUID templateId, UUID versionId, UUID actorId) {
        try {
            return repository.requireVersion(actorId, templateId, versionId);
        } catch (IllegalArgumentException exception) {
            throw new TemplateNotFoundException();
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
