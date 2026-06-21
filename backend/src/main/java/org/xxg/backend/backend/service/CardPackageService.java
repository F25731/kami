package org.xxg.backend.backend.service;

import org.springframework.stereotype.Service;
import org.xxg.backend.backend.entity.CardPackage;
import org.xxg.backend.backend.mapper.CardPackageMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CardPackageService {

    private final CardPackageMapper cardPackageMapper;
    private final ProjectService projectService;

    public CardPackageService(CardPackageMapper cardPackageMapper, ProjectService projectService) {
        this.cardPackageMapper = cardPackageMapper;
        this.projectService = projectService;
    }

    public List<CardPackage> listByProjectId(Long projectId) {
        return cardPackageMapper.findByProjectId(projectId);
    }

    public List<CardPackage> listEnabledByProjectId(Long projectId) {
        return cardPackageMapper.findEnabledByProjectId(projectId);
    }

    public CardPackage getById(Long id) {
        return cardPackageMapper.findById(id);
    }

    public Map<String, Object> create(CardPackage pkg) {
        Map<String, Object> result = new HashMap<>();

        // Project must exist
        if (projectService.getById(pkg.getProjectId()) == null) {
            result.put("success", false);
            result.put("message", "项目不存在");
            return result;
        }

        // package_code unique within project
        if (pkg.getPackageCode() != null && !pkg.getPackageCode().isBlank()) {
            if (cardPackageMapper.existsByProjectIdAndCode(pkg.getProjectId(), pkg.getPackageCode())) {
                result.put("success", false);
                result.put("message", "套餐编码在该项目中已存在");
                return result;
            }
        }

        // at least one of count_value / duration_days / is_permanent must be meaningful
        boolean hasCount = pkg.getCountValue() != null && pkg.getCountValue() > 0;
        boolean hasDuration = pkg.getDurationDays() != null && pkg.getDurationDays() > 0;
        boolean isPermanent = Boolean.TRUE.equals(pkg.getIsPermanent());
        if (!hasCount && !hasDuration && !isPermanent) {
            result.put("success", false);
            result.put("message", "套餐必须设置次数、有效天数或永久有效其中之一");
            return result;
        }

        Long id = cardPackageMapper.insert(pkg);
        result.put("success", true);
        result.put("id", id);
        result.put("data", cardPackageMapper.findById(id));
        return result;
    }

    public Map<String, Object> update(Long id, CardPackage req) {
        Map<String, Object> result = new HashMap<>();
        CardPackage existing = cardPackageMapper.findById(id);
        if (existing == null) {
            result.put("success", false);
            result.put("message", "套餐不存在");
            return result;
        }

        if (req.getPackageCode() != null && !req.getPackageCode().isBlank()) {
            if (cardPackageMapper.existsByProjectIdAndCodeExcludeId(
                    existing.getProjectId(), req.getPackageCode(), id)) {
                result.put("success", false);
                result.put("message", "套餐编码在该项目中已存在");
                return result;
            }
            existing.setPackageCode(req.getPackageCode());
        }
        if (req.getPackageName() != null) existing.setPackageName(req.getPackageName());
        if (req.getCardType() != null) existing.setCardType(req.getCardType());
        if (req.getCountValue() != null) existing.setCountValue(req.getCountValue());
        if (req.getDurationDays() != null) existing.setDurationDays(req.getDurationDays());
        if (req.getIsPermanent() != null) existing.setIsPermanent(req.getIsPermanent());
        if (req.getPrice() != null) existing.setPrice(req.getPrice());
        if (req.getStatus() != null) existing.setStatus(req.getStatus());
        if (req.getSort() != null) existing.setSort(req.getSort());
        if (req.getRemark() != null) existing.setRemark(req.getRemark());

        cardPackageMapper.update(existing);
        result.put("success", true);
        result.put("data", cardPackageMapper.findById(id));
        return result;
    }

    public Map<String, Object> delete(Long id) {
        Map<String, Object> result = new HashMap<>();
        CardPackage pkg = cardPackageMapper.findById(id);
        if (pkg == null) {
            result.put("success", false);
            result.put("message", "套餐不存在");
            return result;
        }
        int cardCount = cardPackageMapper.countCardsByPackageId(id);
        if (cardCount > 0) {
            result.put("success", false);
            result.put("message", "该套餐下已有 " + cardCount + " 张卡密，无法删除，请先禁用");
            return result;
        }
        cardPackageMapper.delete(id);
        result.put("success", true);
        return result;
    }

    public Map<String, Object> updateStatus(Long id, String status) {
        Map<String, Object> result = new HashMap<>();
        if (!List.of("enabled", "disabled").contains(status)) {
            result.put("success", false);
            result.put("message", "状态值无效");
            return result;
        }
        CardPackage pkg = cardPackageMapper.findById(id);
        if (pkg == null) {
            result.put("success", false);
            result.put("message", "套餐不存在");
            return result;
        }
        cardPackageMapper.updateStatus(id, status);
        result.put("success", true);
        return result;
    }
}
