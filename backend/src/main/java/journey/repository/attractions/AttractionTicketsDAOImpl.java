package journey.repository.attractions;

import journey.domain.attractiontickets.AttractionTicketsBean;
import journey.dto.attractions.AttractionSearchDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class AttractionTicketsDAOImpl implements AttractionTicketsDAO {

    @PersistenceContext
    private EntityManager entityManager;

    public EntityManager getSession() {
        return entityManager;
    }

    @Override
    public List<AttractionTicketsBean> find(AttractionSearchDto dto) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<AttractionTicketsBean> cq = cb.createQuery(AttractionTicketsBean.class);
        Root<AttractionTicketsBean> root = cq.from(AttractionTicketsBean.class);

        List<Predicate> predicates = new ArrayList<>();

        

// 🔍 模糊搜尋票券名稱
if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
    predicates.add(cb.like(root.get("name"), "%" + dto.getKeyword() + "%"));
}

// 🗺 國家 ID 篩選
if (dto.getCountryId() != null) {
    predicates.add(cb.equal(root.get("country").get("id"), dto.getCountryId()));
}

// 🏙 城市 ID 篩選
if (dto.getCityId() != null) {
    predicates.add(cb.equal(root.get("city").get("id"), dto.getCityId()));
}

// ✅ 啟用狀態（建議 dir 改為 state，語意更清楚）
if (dto.getDir() != null) {
    predicates.add(cb.equal(root.get("state"), dto.getDir()));
}

// ===== 多對多 JOIN 條件處理 =====
if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
    Join<Object, Object> tagsJoin = root.join("attractionTags", JoinType.LEFT);
    predicates.add(tagsJoin.get("id").in(dto.getTagIds()));
    cq.distinct(true); // ✅ 避免 JOIN 重複導致回傳重複資料
}

if (dto.getTypeIds() != null && !dto.getTypeIds().isEmpty()) {
    Join<Object, Object> typesJoin = root.join("attractionTypes", JoinType.LEFT);
    predicates.add(typesJoin.get("id").in(dto.getTypeIds()));
    cq.distinct(true);
}

// ✅ 所有條件建完之後才放進 query
cq.where(predicates.toArray(new Predicate[0]));

// ⬆⬇ 排序（限白名單欄位）
String orderField = dto.getOrder() != null ? dto.getOrder() : "createdAt";
boolean isDesc = dto.getDir() != null && dto.getDir(); // true = desc
if (isSafeOrderField(orderField)) {
    if (isDesc) {
        cq.orderBy(cb.desc(root.get(orderField)));
    } else {
        cq.orderBy(cb.asc(root.get(orderField)));
    }
}

        // 📄 查詢分頁處理
        int start = dto.getStart() != null ? dto.getStart() : 0;
        int rows = dto.getRows() != null ? dto.getRows() : 10;

        TypedQuery<AttractionTicketsBean> query = getSession().createQuery(cq);
        query.setFirstResult(start);
        query.setMaxResults(rows);

        return query.getResultList();
    }

    @Override
    public long count(AttractionSearchDto dto) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<AttractionTicketsBean> root = cq.from(AttractionTicketsBean.class);

        List<Predicate> predicates = new ArrayList<>();

        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            predicates.add(cb.like(root.get("name"), "%" + dto.getKeyword() + "%"));
        }

        if (dto.getCountryId() != null) {
            predicates.add(cb.equal(root.get("country").get("id"), dto.getCountryId()));
        }

        if (dto.getCityId() != null) {
            predicates.add(cb.equal(root.get("city").get("id"), dto.getCityId()));
        }

        if (dto.getDir() != null) {
            predicates.add(cb.equal(root.get("state"), dto.getDir()));
        }

        cq.select(cb.count(root));
        cq.where(predicates.toArray(new Predicate[0]));

        return getSession().createQuery(cq).getSingleResult();
    }

    // 🔐 排序欄位白名單，避免 SQL injection
    private boolean isSafeOrderField(String field) {
        return field.equals("createdAt") ||
               field.equals("price") ||
               field.equals("views") ||
               field.equals("updatedAt");
    }
}
