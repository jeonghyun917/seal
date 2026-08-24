package kr.or.arte.seal.recipient.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class RecipientForm {
    @NotNull private UUID organizationUnitId;
    @Size(max = 100) private String name;
    @Size(max = 254) private String email;
    @NotBlank @Size(min = 5, max = 500) private String collectionBasis;
    private long versionNo;

    public UUID getOrganizationUnitId() { return organizationUnitId; }
    public void setOrganizationUnitId(UUID value) { organizationUnitId = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getCollectionBasis() { return collectionBasis; }
    public void setCollectionBasis(String value) { collectionBasis = value; }
    public long getVersionNo() { return versionNo; }
    public void setVersionNo(long value) { versionNo = value; }
}
