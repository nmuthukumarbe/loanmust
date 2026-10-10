package com.loanmust.loan.builder;

import com.loanmust.loan.audit.AuditService;
import com.loanmust.loan.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Builder tie-ups and their projects. */
@Service
public class BuilderService {

    private final BuilderRepository builderRepository;
    private final ProjectRepository projectRepository;
    private final AuditService auditService;

    public BuilderService(BuilderRepository builderRepository, ProjectRepository projectRepository,
                          AuditService auditService) {
        this.builderRepository = builderRepository;
        this.projectRepository = projectRepository;
        this.auditService = auditService;
    }

    public List<Builder> builders(Long tenantId) {
        return builderRepository.findByTenantIdAndDeletedFalseOrderByNameAsc(tenantId);
    }

    public List<Builder> activeBuilders(Long tenantId) {
        return builderRepository.findByTenantIdAndActiveTrueAndDeletedFalseOrderByNameAsc(tenantId);
    }

    public Builder get(Long tenantId, Long id) {
        return id == null ? null : builderRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElse(null);
    }

    public Project getProject(Long tenantId, Long id) {
        return id == null ? null : projectRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId).orElse(null);
    }

    public List<Project> projects(Long tenantId, Long builderId) {
        return projectRepository.findByTenantIdAndBuilderIdAndDeletedFalseOrderByNameAsc(tenantId, builderId);
    }

    public List<Project> allProjects(Long tenantId) {
        return projectRepository.findByTenantIdAndDeletedFalseOrderByBuilderNameAscNameAsc(tenantId);
    }

    public long projectCount(Long tenantId, Long builderId) {
        return projectRepository.countByTenantIdAndBuilderIdAndDeletedFalse(tenantId, builderId);
    }

    @Transactional
    public Builder save(Long tenantId, Builder form) {
        boolean isNew = form.getId() == null;
        Builder entity = isNew ? new Builder() : get(tenantId, form.getId());
        if (entity == null) return null;
        if (isNew) {
            entity.setTenantId(tenantId);
            entity.setCreatedBy(SecurityUtils.currentUserLogin());
        }
        AuditService.Diff diff = new AuditService.Diff()
                .compare("Name", entity.getName(), form.getName())
                .compare("Contact", entity.getContactPerson(), form.getContactPerson())
                .compare("Arrangement", entity.getArrangement(), form.getArrangement())
                .compare("Active", entity.isActive(), form.isActive());
        entity.setName(form.getName());
        entity.setContactPerson(form.getContactPerson());
        entity.setMobile(form.getMobile());
        entity.setEmail(form.getEmail());
        entity.setAddress(form.getAddress());
        entity.setCity(form.getCity());
        entity.setTieUpSince(form.getTieUpSince());
        entity.setArrangement(form.getArrangement());
        entity.setActive(form.isActive());
        entity.setNotes(form.getNotes());
        entity.setModifiedBy(SecurityUtils.currentUserLogin());
        Builder saved = builderRepository.save(entity);
        if (isNew) {
            auditService.log("CREATE", "Builder", saved.getId(), saved.getName(), "Builder tie-up added");
        } else {
            auditService.logDiff("Builder", saved.getId(), saved.getName(), diff);
            for (Project p : projects(tenantId, saved.getId())) {
                if (!saved.getName().equals(p.getBuilderName())) {
                    p.setBuilderName(saved.getName());
                    projectRepository.save(p);
                }
            }
        }
        return saved;
    }

    @Transactional
    public void archive(Long tenantId, Long id) {
        Builder b = get(tenantId, id);
        if (b == null) return;
        b.setDeleted(true);
        b.setActive(false);
        b.setModifiedBy(SecurityUtils.currentUserLogin());
        builderRepository.save(b);
        auditService.log("ARCHIVE", "Builder", id, b.getName(), "Builder archived");
    }

    @Transactional
    public Project saveProject(Long tenantId, Project form) {
        Builder builder = get(tenantId, form.getBuilderId());
        if (builder == null) return null;
        boolean isNew = form.getId() == null;
        Project entity = isNew ? new Project() : getProject(tenantId, form.getId());
        if (entity == null) return null;
        if (isNew) {
            entity.setTenantId(tenantId);
            entity.setCreatedBy(SecurityUtils.currentUserLogin());
        }
        AuditService.Diff diff = new AuditService.Diff()
                .compare("Name", entity.getName(), form.getName())
                .compare("Status", entity.getStatus(), form.getStatus())
                .compare("Approved lenders", entity.getApprovedLenders(), form.getApprovedLenders());
        entity.setBuilderId(builder.getId());
        entity.setBuilderName(builder.getName());
        entity.setName(form.getName());
        entity.setLocation(form.getLocation());
        entity.setCity(form.getCity());
        entity.setReraNumber(form.getReraNumber());
        entity.setProjectType(form.getProjectType());
        entity.setStatus(form.getStatus());
        entity.setTotalUnits(form.getTotalUnits());
        entity.setPriceFrom(form.getPriceFrom());
        entity.setPriceTo(form.getPriceTo());
        entity.setPossessionDate(form.getPossessionDate());
        entity.setApprovedLenders(form.getApprovedLenders());
        entity.setNotes(form.getNotes());
        entity.setModifiedBy(SecurityUtils.currentUserLogin());
        Project saved = projectRepository.save(entity);
        if (isNew) {
            auditService.log("CREATE", "Project", saved.getId(), builder.getName() + " · " + saved.getName(), "Project added");
        } else {
            auditService.logDiff("Project", saved.getId(), builder.getName() + " · " + saved.getName(), diff);
        }
        return saved;
    }

    @Transactional
    public void archiveProject(Long tenantId, Long id) {
        Project p = getProject(tenantId, id);
        if (p == null) return;
        p.setDeleted(true);
        p.setModifiedBy(SecurityUtils.currentUserLogin());
        projectRepository.save(p);
        auditService.log("ARCHIVE", "Project", id, p.getName(), "Project archived");
    }
}
