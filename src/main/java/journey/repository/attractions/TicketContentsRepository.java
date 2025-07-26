package journey.repository.attractions;

import journey.domain.attractiontickets.TicketContentsBean;
import journey.domain.attractiontickets.AttractionTicketsBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketContentsRepository extends JpaRepository<TicketContentsBean, Integer> {

    // 根據票券查詢所有內容（通常是多筆）
    List<TicketContentsBean> findByTicket(AttractionTicketsBean ticket);
    
    // 也可根據票券 ID 查詢（若你不想傳 entity）
    // 🔍 查詢某票券下的所有內文
    List<TicketContentsBean> findByTicket_IdOrderByContentIdAsc(Integer ticketId);

}
