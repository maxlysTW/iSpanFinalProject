package journey.controller.attractions;

import journey.domain.attractiontickets.AttractionTicketsBean;
import journey.dto.attractions.AttractionSearchDto;
import journey.dto.attractions.AttractionTicketDto;
import journey.dto.attractions.AttractionTicketSimpleDto;
import journey.service.attractions.AttractionTicketsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import journey.dto.attractions.AttractionTicketMainDto;
import java.util.List;

@RestController
@RequestMapping("/api/attractions/tickets")
public class AttractionTicketsController {

    @Autowired
    private AttractionTicketsService attractionTicketsService;

    /**
     * 🔍 查詢票券（支援條件、分頁、模糊搜尋）
     */
    @PostMapping("/search")
    public List<AttractionTicketsBean> searchTickets(@RequestBody AttractionSearchDto dto) {
        return attractionTicketsService.find(dto);
    }

    /**
     * 📊 計算票券總數（分頁用）
     */
    @PostMapping("/count")
    public long countTickets(@RequestBody AttractionSearchDto dto) {
        return attractionTicketsService.count(dto);
    }

    /**
     * ➕ 單純建立票券主表（不含子表）
     */
    @PostMapping("/createTicket")
    public AttractionTicketsBean createTicket(@RequestBody AttractionTicketDto dto) {
    return attractionTicketsService.create(dto);
}
    //修改主表
    @PutMapping("/{id}")
    public AttractionTicketsBean updateTicket(@PathVariable Integer id,
                                        @RequestBody AttractionTicketDto dto) {
    return attractionTicketsService.update(id, dto);
}

    /**
     * ➕ 建立票券（包含子表）
     */
    @PostMapping("/full")
    public AttractionTicketsBean createFullTicket(@RequestBody AttractionTicketDto dto) {
        return attractionTicketsService.createFull(dto);
    }

    /**
     * 🔎 查詢單一票券
     */
    @GetMapping("/{id:\\d+}")
    public AttractionTicketsBean getTicket(@PathVariable Integer id) {
        return attractionTicketsService.findById(id);
    }

    /**
     * ❌ 刪除票券
     */
    @DeleteMapping("/{id}")
    public boolean deleteTicket(@PathVariable Integer id) {
        return attractionTicketsService.delete(id);
    }
    //商品卡片
    @PostMapping("/simple")
    public List<AttractionTicketSimpleDto> findSimpleTickets(@RequestBody AttractionSearchDto dto) {
    return attractionTicketsService.findSimpleTickets(dto);
    }
    @PostMapping("/search/simple")
    public List<AttractionTicketSimpleDto> searchSimple(@RequestBody AttractionSearchDto dto) {
    return attractionTicketsService.findSimpleTickets(dto);
}
    //商品內頁
    @GetMapping("/main/{id}")
    public AttractionTicketMainDto getMainTicketById(@PathVariable Integer id) {
        return attractionTicketsService.findMainTicketDtoById(id);
    }

    
}
