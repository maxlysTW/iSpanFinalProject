package journey.controller.attractions;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import journey.domain.attractiontickets.AttractionImagesBean;
import journey.dto.attractions.AttractionImageCreateDto;
import journey.service.attractions.AttractionImageService;

@RestController
@RequestMapping("/api/attraction/images")
// @CrossOrigin(origins = "http://192.168.36.96:5173") // ⬅️ 可根據你的前端調整
public class AttractionImageController {

    @Autowired
    private AttractionImageService imageService;

    /**
     * 🔍 查詢指定景點的所有圖片
     */
    @GetMapping("/by-attraction/{attractionId}")
    public List<AttractionImagesBean> getImagesByAttraction(@PathVariable Integer attractionId) {
        return imageService.findByAttractionId(attractionId);
    }

    /**
     * ➕ 新增圖片至指定景點
     */
    @PostMapping("/add")
    public AttractionImagesBean addImage(@RequestBody AttractionImageCreateDto dto) {
        return imageService.addImage(dto);
    }

    /**
     * ✏️ 修改圖片
     */
    @PutMapping("/{imageId}")
    public AttractionImagesBean updateImage(
            @PathVariable Integer imageId,
            @RequestBody AttractionImageCreateDto dto) {
        return imageService.updateImage(imageId, dto);
    }

    /**
     * ❌ 刪除圖片
     */
    @DeleteMapping("/{imageId}")
    public boolean deleteImage(@PathVariable Integer imageId) {
        return imageService.deleteImage(imageId);
    }
}
