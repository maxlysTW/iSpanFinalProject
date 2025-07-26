package journey.service.attractions;

import journey.domain.attractiontickets.TicketImagesBean;
import journey.dto.attractions.TicketImageDto;
import journey.domain.attractiontickets.AttractionTicketsBean;
import journey.repository.attractions.TicketImagesRepository;
import journey.repository.attractions.AttractionTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TicketImageService {

    @Autowired
    private TicketImagesRepository ticketImagesRepository;

    @Autowired
    private AttractionTicketRepository attractionTicketRepository;

    /**
     * ➕ 新增圖片
     */
    public TicketImagesBean create(TicketImageDto dto) {
        AttractionTicketsBean ticket = attractionTicketRepository.findById(dto.getTicketId())
                .orElseThrow(() -> new RuntimeException("找不到票券 ID: " + dto.getTicketId()));

        TicketImagesBean image = new TicketImagesBean();
        image.setTicket(ticket);
        image.setImageUrl(dto.getImageUrl());
        image.setImageType(dto.getImageType());
        image.setSortOrder(dto.getSortOrder());

        return ticketImagesRepository.save(image);
    }

    /**
     * ✏️ 修改圖片
     */
    public TicketImagesBean update(Integer id, TicketImageDto dto) {
        TicketImagesBean image = ticketImagesRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("找不到圖片 ID: " + id));

        image.setImageUrl(dto.getImageUrl());
        image.setImageType(dto.getImageType());
        image.setSortOrder(dto.getSortOrder());

        return ticketImagesRepository.save(image);
    }

    /**
     * ❌ 刪除圖片
     */
    public boolean delete(Integer id) {
        if (ticketImagesRepository.existsById(id)) {
            ticketImagesRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * 🔍 查詢指定票券的所有圖片
     */
    public List<TicketImagesBean> findByTicketId(Integer ticketId) {
        return ticketImagesRepository.findByTicketId(ticketId);
    }

    /**
     * 🔍 查詢單筆圖片
     */
    public TicketImagesBean findById(Integer id) {
        Optional<TicketImagesBean> optional = ticketImagesRepository.findById(id);
        return optional.orElse(null);
    }
}

