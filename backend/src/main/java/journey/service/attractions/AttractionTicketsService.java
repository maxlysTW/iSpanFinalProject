package journey.service.attractions;

import journey.domain.attractiontickets.*;
import journey.domain.locations.CityBean;
import journey.domain.locations.CountryBean;

import journey.dto.attractions.AttractionSearchDto;
import journey.dto.attractions.AttractionTicketDto;
import journey.dto.attractions.AttractionTicketMainDto;
import journey.dto.attractions.AttractionTicketSimpleDto;
import journey.dto.attractions.TicketContentDto;
import journey.dto.attractions.TicketImageDto;
import journey.dto.attractions.TicketPackageDto;
import journey.dto.attractions.TicketTypeDto;
import journey.repository.attractions.*;
import journey.repository.locations.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AttractionTicketsService {
    @Autowired private AttractionTicketMainDAO attractionTicketsMainDAO;

    @Autowired private AttractionTicketsDAO attractionTicketsDAO;
    @Autowired private AttractionTicketRepository attractionTicketRepository;
    @Autowired private TicketImagesRepository ticketImagesRepository;
    @Autowired private TicketContentsRepository ticketContentsRepository;
    @Autowired private TicketPackagesRepository ticketPackagesRepository;
    @Autowired private TicketTypesRepository ticketTypesRepository;
    @Autowired private CityRepository cityRepository;
    @Autowired private CountryRepository countryRepository;
    
    @Autowired private AttractionTypesRepository attractionTypesRepository;
    @Autowired private AttractionTagsRepository attractionTagsRepository;
    // 🔍 多條件查詢票券資料
    public List<AttractionTicketsBean> find(AttractionSearchDto dto) {
        return attractionTicketsDAO.find(dto);
    }

    // 📊 分頁查詢數量
    public long count(AttractionSearchDto dto) {
        return attractionTicketsDAO.count(dto);
    }

    // ✅ ➕ 使用 DTO 建立主表資料（不含子表）
public AttractionTicketsBean create(AttractionTicketDto dto) {
    AttractionTicketsBean ticket = new AttractionTicketsBean();

    ticket.setName(dto.getName());
    ticket.setDescription(dto.getDescription());
    ticket.setAddress(dto.getAddress());
    ticket.setImageUrl(dto.getImageUrl());
    ticket.setImageType(dto.getImageType());
    ticket.setViews(dto.getViews());
    ticket.setState(dto.getState());
    ticket.setStartTime(dto.getStartTime());
    ticket.setEndTime(dto.getEndTime());
    ticket.setCreatedAt(LocalDateTime.now());
    ticket.setCreatedBy(dto.getCreatedBy());

    // 👉 設定國家、城市（多對一）
    ticket.setCountry(countryRepository.findById(dto.getCountryId()).orElse(null));
    ticket.setCity(cityRepository.findById(dto.getCityId()).orElse(null));

    // 👉 設定類型（多對多）
    if (dto.getTypeIds() != null) {
        ticket.setAttractionTypes(new HashSet<>(attractionTypesRepository.findAllById(dto.getTypeIds())));
    }

    // 👉 設定標籤（多對多）
    if (dto.getTagIds() != null) {
        ticket.setAttractionTags(new HashSet<>(attractionTagsRepository.findAllById(dto.getTagIds())));
    }

    return attractionTicketRepository.save(ticket);
}
    // ✏️ 使用 DTO 修改票券主表（不含子表）
public AttractionTicketsBean update(Integer id, AttractionTicketDto dto) {
    AttractionTicketsBean ticket = attractionTicketRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("找不到指定票券 ID：" + id));

    ticket.setName(dto.getName());
    ticket.setDescription(dto.getDescription());
    ticket.setAddress(dto.getAddress());
    ticket.setImageUrl(dto.getImageUrl());
    ticket.setImageType(dto.getImageType());
    ticket.setViews(dto.getViews());
    ticket.setState(dto.getState());
    ticket.setStartTime(dto.getStartTime());
    ticket.setEndTime(dto.getEndTime());
    ticket.setUpdatedAt(LocalDateTime.now());
    ticket.setUpdatedBy(dto.getUpdatedBy());

    // 多對一
    CountryBean country = countryRepository.findById(dto.getCountryId())
        .orElseThrow(() -> new RuntimeException("找不到國家"));
    CityBean city = cityRepository.findById(dto.getCityId())
        .orElseThrow(() -> new RuntimeException("找不到城市"));

    ticket.setCountry(country);
    ticket.setCity(city);

    // 多對多
    if (dto.getTagIds() != null) {
        Set<AttractionTagsBean> tags = new HashSet<>(attractionTagsRepository.findAllById(dto.getTagIds()));
        ticket.setAttractionTags(tags);
    }

    if (dto.getTypeIds() != null) {
        Set<AttractionTypesBean> types = new HashSet<>(attractionTypesRepository.findAllById(dto.getTypeIds()));
        ticket.setAttractionTypes(types);
    }

    return attractionTicketRepository.save(ticket);
}

    // ❌ 刪除票券主表
    public boolean delete(Integer id) {
        if (attractionTicketRepository.existsById(id)) {
            attractionTicketRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // 🔎 查詢單一票券
    public AttractionTicketsBean findById(Integer id) {
        Optional<AttractionTicketsBean> optional = attractionTicketRepository.findById(id);
        return optional.orElse(null);
    }
    //全建票券
    public AttractionTicketsBean createFull(AttractionTicketDto dto) {
        AttractionTicketsBean ticket = new AttractionTicketsBean();
        ticket.setName(dto.getName());
        ticket.setDescription(dto.getDescription());
        ticket.setAddress(dto.getAddress());
        ticket.setImageUrl(dto.getImageUrl());
        ticket.setImageType(dto.getImageType());
        ticket.setViews(dto.getViews() != null ? dto.getViews() : 0);
        ticket.setState(dto.getState());
        ticket.setStartTime(dto.getStartTime());
        ticket.setEndTime(dto.getEndTime());
        ticket.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : LocalDateTime.now());
        ticket.setCreatedBy(dto.getCreatedBy());
    
        // 🔗 設定城市與國家
        ticket.setCity(cityRepository.findById(dto.getCityId()).orElse(null));
        ticket.setCountry(countryRepository.findById(dto.getCountryId()).orElse(null));
    
        // 🔗 多對多設定：標籤 / 類型
        if (dto.getTagIds() != null) {
            Set<AttractionTagsBean> tags = new HashSet<>(attractionTagsRepository.findAllById(dto.getTagIds()));
            ticket.setAttractionTags(tags);
        }
        if (dto.getTypeIds() != null) {
            Set<AttractionTypesBean> types = new HashSet<>(attractionTypesRepository.findAllById(dto.getTypeIds()));
            ticket.setAttractionTypes(types);
        }
    
        // 💾 儲存主表
        AttractionTicketsBean savedTicket = attractionTicketRepository.save(ticket);
    
        // 💾 儲存圖片
        if (dto.getImages() != null) {
            for (TicketImageDto imageDto : dto.getImages()) {
                TicketImagesBean img = new TicketImagesBean();
                img.setTicket(savedTicket);
                img.setImageUrl(imageDto.getImageUrl());
                img.setImageType(imageDto.getImageType());
                
                img.setSortOrder(imageDto.getSortOrder());
                ticketImagesRepository.save(img);
            }
        }
    
        // 💾 儲存內文
        if (dto.getContents() != null) {
            for (TicketContentDto contentDto : dto.getContents()) {
                TicketContentsBean content = new TicketContentsBean();
                content.setTicket(savedTicket);
                content.setTitle(contentDto.getTitle());
                content.setSubtitle(contentDto.getSubtitle());
                content.setDescription(contentDto.getDescription());
                ticketContentsRepository.save(content);
            }
        }
    
        // // 💾 儲存套票與票種
        if (dto.getPackages() != null) {
            for (TicketPackageDto pkgDto : dto.getPackages()) {
                TicketPackagesBean pkg = new TicketPackagesBean();
                pkg.setTicket(savedTicket);
                pkg.setName(pkgDto.getName());
                pkg.setDescription(pkgDto.getDescription());
                pkg.setImageUrl(pkgDto.getImageUrl());
                pkg.setImageType(pkgDto.getImageType());
                pkg.setStartTime(pkgDto.getStartTime());
                pkg.setEndTime(pkgDto.getEndTime());
                pkg.setCreatedBy(dto.getCreatedBy());
                pkg.setCreatedAt(LocalDateTime.now());
                pkg.setState(pkgDto.getState());
    
                TicketPackagesBean savedPkg = ticketPackagesRepository.save(pkg);
    
        //         // 票種
                if (pkgDto.getTypes() != null) {
                    for (TicketTypeDto typeDto : pkgDto.getTypes()) {
                        TicketTypesBean type = new TicketTypesBean();
                        type.setTicketPackage(savedPkg);
                        type.setTicketName(typeDto.getTicketName());
                        type.setPrice(typeDto.getPrice());
                        type.setQuantity(typeDto.getQuantity());
                        type.setDate(typeDto.getDate());
                        ticketTypesRepository.save(type);
                    }
                }
            }
        }
    
        // // ✅ 回傳前補上查詢以帶出子資料
        savedTicket.setImages(new HashSet<>(ticketImagesRepository.findByTicketId(savedTicket.getId())));
        savedTicket.setContents(new HashSet<>(ticketContentsRepository.findByTicket_IdOrderByContentIdAsc(savedTicket.getId())));
        savedTicket.setPackages(new HashSet<>(ticketPackagesRepository.findByTicket_Id(savedTicket.getId())));
        
        for (TicketPackagesBean pkg : savedTicket.getPackages()) {
            pkg.setTypes(new HashSet<>(ticketTypesRepository.findByTicketPackage_Id(pkg.getId())));
        }
        return savedTicket;
    }
          // 🔍 查詢簡易票券資訊（主表 + 最低票價）
    public List<AttractionTicketSimpleDto> findSimpleTickets(AttractionSearchDto dto) {
        List<AttractionTicketsBean> tickets = attractionTicketsDAO.find(dto);
        return tickets.stream().map(ticket -> {
            Integer minPrice = ticket.getPackages().stream()
                .flatMap(pkg -> pkg.getTypes().stream())
                .map(TicketTypesBean::getPrice)
                .min(Integer::compareTo)
                .orElse(0);

            return new AttractionTicketSimpleDto(
                ticket.getId(),
                ticket.getName(),
                ticket.getImageUrl(),
                ticket.getAddress(),
                ticket.getDescription(),
                minPrice,
                ticket.getViews()
            );
        }).collect(Collectors.toList());
    }
    // 🔍 查詢主表資訊用於商品內頁（不含圖片、評論、FAQ 等大資料）
   public AttractionTicketMainDto findMainTicketDtoById(Integer id) {
    AttractionTicketsBean ticket = attractionTicketRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("找不到票券 ID：" + id));

    AttractionTicketMainDto dto = new AttractionTicketMainDto();
    dto.setId(ticket.getId());
    dto.setName(ticket.getName());
    dto.setDescription(ticket.getDescription());
    dto.setAddress(ticket.getAddress());
    dto.setImageUrl(ticket.getImageUrl());
    dto.setViews(ticket.getViews());
    dto.setCreatedBy(ticket.getCreatedBy());
    if (ticket.getCountry() != null) {
        dto.setCountryName(ticket.getCountry().getNameZh());
    }
    if (ticket.getCity() != null) {
        dto.setCityName(ticket.getCity().getNameZh());
    }
    if (ticket.getAttractionTags() != null) {
        dto.setTagNames(ticket.getAttractionTags().stream()
            .map(tag -> tag.getTagName())
            .collect(Collectors.toList()));
    }
    if (ticket.getAttractionTypes() != null) {
        dto.setTypeNames(ticket.getAttractionTypes().stream()
            .map(type -> type.getType())
            .collect(Collectors.toList()));
    }

    return dto;
}
    //
}
