package journey.controller.attractions;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import journey.domain.attractiontickets.AttractionsBean;
import journey.dto.attractions.AttractionCreateDto;
import journey.dto.attractions.AttractionFullCreateDto;
import journey.dto.attractions.AttractionSearchDto;
import journey.dto.attractions.AttractionUpdateDto;
import journey.service.attractions.AttractionsService;

@RestController
@RequestMapping("/api/attractions") // ⛳ 所有景點功能的 API 會以此為前綴
// @CrossOrigin(origins = "http://192.168.36.96:5173") // 🎯 允許前端開發機跨域請求（Vite）
public class AttractionsController {

    @Autowired
    private AttractionsService attractionsService;

    /**
     * 🔍 查詢景點列表
     * - 支援條件：關鍵字、國家、城市、類型、標籤
     * - 支援排序與分頁
     */
    @PostMapping("/search")
    public List<AttractionsBean> search(@RequestBody AttractionSearchDto dto) {
        return attractionsService.search(dto);
    }

    /**
     * ➕ 新增景點（基本資訊，不含圖片與介紹）
     * - 使用 DTO 接收輸入，比直接用實體更安全
     */
    @PostMapping("/new")
    public AttractionsBean create(@RequestBody AttractionCreateDto dto) {
        return attractionsService.create(dto); // ✅ 改為接收 dto 並呼叫相應 service 方法
    }

    /**
     * 🧩 新增完整景點（含圖片與內文）
     * - 使用 DTO 接收完整結構
     */
    @PostMapping("/full")
    public AttractionsBean createFull(@RequestBody AttractionFullCreateDto dto) {
        return attractionsService.createFull(dto);
    }

    /**
     * ✏️ 修改指定 ID 的景點資料（只修改主表，不含圖片與介紹）
     */
    @PutMapping("/{id}")
    public AttractionsBean update(@PathVariable Integer id, @RequestBody AttractionUpdateDto dto) {
        return attractionsService.update(id, dto);
    }

    /**
     * ❌ 刪除景點資料（主表）
     * - 若資料存在則刪除並回傳 true，否則回傳 false
     */
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Integer id) {
        return attractionsService.delete(id);
    }

    // 🔍 查詢全部景點（含子表）
    @PostMapping("/all-with-details")
    public List<AttractionsBean> findAllWithDetails() {
        return attractionsService.findAllWithDetails();
    }
}
