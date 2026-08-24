package kr.or.arte.seal.template.application;

import java.util.UUID;

public record PublishedConsentTemplateOption(UUID templateVersionId, String templateCode,
                                             String templateName, String activityName,
                                             int versionNumber) { }
