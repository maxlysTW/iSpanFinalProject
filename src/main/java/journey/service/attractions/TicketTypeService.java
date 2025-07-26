package journey.service.attractions;

import journey.domain.attractiontickets.TicketPackagesBean;
import journey.domain.attractiontickets.TicketTypesBean;
import journey.dto.attractions.TicketTypeDto;
import journey.repository.attractions.TicketPackagesRepository;
import journey.repository.attractions.TicketTypesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketTypeService {

    @Autowired
    private TicketTypesRepository ticketTypesRepository;

    @Autowired
    private TicketPackagesRepository ticketPackagesRepository;

    /**
     * ➕ 新增票種
     */
    public TicketTypesBean create(TicketTypeDto dto) {
        TicketPackagesBean pkg = ticketPackagesRepository.findById(dto.getPackageId())
                .orElseThrow(() -> new RuntimeException("找不到套票 ID: " + dto.getPackageId()));

        TicketTypesBean type = new TicketTypesBean();
        type.setTicketPackage(pkg);
        type.setTicketName(dto.getTicketName());
        type.setPrice(dto.getPrice());
        type.setQuantity(dto.getQuantity());
        type.setDate(dto.getDate());

        return ticketTypesRepository.save(type);
    }

    /**
     * ✏️ 修改票種
     */
    public TicketTypesBean update(Integer id, TicketTypeDto dto) {
        TicketTypesBean type = ticketTypesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到票種 ID: " + id));

        type.setTicketName(dto.getTicketName());
        type.setPrice(dto.getPrice());
        type.setQuantity(dto.getQuantity());
        type.setDate(dto.getDate());

        return ticketTypesRepository.save(type);
    }

    /**
     * ❌ 刪除票種
     */
    public boolean delete(Integer id) {
        if (ticketTypesRepository.existsById(id)) {
            ticketTypesRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * 🔍 查詢票種（單一）
     */
    public TicketTypesBean findById(Integer id) {
        return ticketTypesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到票種 ID: " + id));
    }

    /**
     * 🔍 查詢某套票下的所有票種
     */
    public List<TicketTypesBean> findByPackageId(Integer packageId) {
        return ticketTypesRepository.findByTicketPackage_Id(packageId);
    }
}

