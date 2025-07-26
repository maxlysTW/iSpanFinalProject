package journey.repository.attractions;

import journey.domain.attractiontickets.AttractionTagsBean;
import journey.domain.attractiontickets.AttractionTypesBean;
import journey.domain.attractiontickets.AttractionsBean;
import journey.dto.attractions.AttractionSearchDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class AttractionsDAOImpl implements AttractionsDAO {

    @PersistenceContext
    private EntityManager entityManager;

    public EntityManager getSession() {
        return entityManager;
    }

    @Override
    public List<AttractionsBean> find(AttractionSearchDto dto) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<AttractionsBean> cq = cb.createQuery(AttractionsBean.class);
        Root<AttractionsBean> root = cq.from(AttractionsBean.class);

        List<Predicate> predicates = new ArrayList<>();

        // 🔍 關鍵字模糊搜尋（支援多關鍵字）
        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            String[] terms = dto.getKeyword().split("\\s+");
            List<Predicate> orGroup = new ArrayList<>();
            for (String term : terms) {
                orGroup.add(cb.like(root.get("name"), "%" + term + "%"));
            }
            predicates.add(cb.or(orGroup.toArray(new Predicate[0])));
        }

        // 🌎 篩選：國家 ID
        if (dto.getCountryId() != null) {
            predicates.add(cb.equal(root.get("country").get("id"), dto.getCountryId()));
        }

        // 🏙 篩選：城市 ID
        if (dto.getCityId() != null) {
            predicates.add(cb.equal(root.get("city").get("id"), dto.getCityId()));
        }

        // 📚 篩選：景點類型（多對多 JOIN）
        if (dto.getTypeIds() != null) {
            Join<AttractionsBean, AttractionTypesBean> join = root.join("types");
            predicates.add(cb.equal(join.get("id"), dto.getTypeIds()));
        }

        // 🏷 篩選：標籤（多對多 JOIN）
        if (dto.getTagIds() != null) {
            Join<AttractionsBean, AttractionTagsBean> join = root.join("tags");
            predicates.add(cb.equal(join.get("id"), dto.getTagIds()));
        }

        // 🔗 套用所有條件
        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        // ⬆⬇ 排序邏輯
        String orderBy = dto.getOrder() != null ? dto.getOrder() : "id";
        boolean desc = dto.getDir() != null && dto.getDir();
        if (desc) {
            cq.orderBy(cb.desc(root.get(orderBy)));
        } else {
            cq.orderBy(cb.asc(root.get(orderBy)));
        }

        // 🔢 分頁
        int start = dto.getStart() != null ? dto.getStart() : 0;
        int rows = dto.getRows() != null ? dto.getRows() : 10;

        TypedQuery<AttractionsBean> query = getSession().createQuery(cq);
        query.setFirstResult(start);
        query.setMaxResults(rows);

        return query.getResultList();
    }

    @Override
    public long count(AttractionSearchDto dto) {
        CriteriaBuilder cb = getSession().getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<AttractionsBean> root = cq.from(AttractionsBean.class);
        cq.select(cb.count(root));

        List<Predicate> predicates = new ArrayList<>();

        // 🔍 關鍵字模糊搜尋
        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            String[] terms = dto.getKeyword().split("\\s+");
            List<Predicate> orGroup = new ArrayList<>();
            for (String term : terms) {
                orGroup.add(cb.like(root.get("name"), "%" + term + "%"));
            }
            predicates.add(cb.or(orGroup.toArray(new Predicate[0])));
        }

        if (dto.getCountryId() != null) {
            predicates.add(cb.equal(root.get("country").get("id"), dto.getCountryId()));
        }

        if (dto.getCityId() != null) {
            predicates.add(cb.equal(root.get("city").get("id"), dto.getCityId()));
        }

        if (dto.getTypeIds() != null) {
            Join<AttractionsBean, AttractionTypesBean> join = root.join("types");
            predicates.add(cb.equal(join.get("id"), dto.getTypeIds()));
        }

        if (dto.getTagIds() != null) {
            Join<AttractionsBean, AttractionTagsBean> join = root.join("tags");
            predicates.add(cb.equal(join.get("id"), dto.getTagIds()));
        }

        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        return getSession().createQuery(cq).getSingleResult();
    }
}