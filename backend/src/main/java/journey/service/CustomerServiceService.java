package journey.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import journey.domain.onlinecustomerservice.CustomerServiceChatroomBean;
import journey.domain.onlinecustomerservice.CustomerServiceMessageBean;
import journey.domain.onlinecustomerservice.CustomerServiceQuestionBean;
import journey.domain.payments.TypeEnumBean;
import journey.domain.users.AdminBean;
import journey.domain.users.UserBean;
import journey.dto.MessageResponseDTO;
import journey.dto.QuestionTemplateDTO;
import journey.repository.onlinecustomerservice.CustomerServiceChatroomRepository;
import journey.repository.onlinecustomerservice.CustomerServiceMessageRepository;
import journey.repository.onlinecustomerservice.CustomerServiceQuestionRepository;
import journey.repository.payments.TypeEnumRepository;
import journey.repository.users.AdminRepository;
import journey.repository.users.UserRepository;

@Service
public class CustomerServiceService {

    @Autowired
    private CustomerServiceChatroomRepository chatroomRepository;

    @Autowired
    private CustomerServiceMessageRepository messageRepository;

    @Autowired
    private CustomerServiceQuestionRepository questionRepository;

    @Autowired
    private TypeEnumRepository typeEnumRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    // 這邊先寫死，之後要根據JWT驗證來更新，寫好完整的方法 !!!!!!!!!!!!!!!
    // 您有一個方法來獲取當前登入的用戶ID
    // 實際應用中這會從 Spring Security 或 Session 中獲取
    private Integer getLoggedInUserId() {
        // TODO: 這裡應該從實際的用戶會話或安全上下文中獲取當前用戶ID
        // 為了演示，假設用戶ID為1 ------------ 先寫死
        return 1;
    }

    // 獲取(查出)或創建聊天室
    @Transactional
    public CustomerServiceChatroomBean getOrCreateChatroom(Integer typeId) {
        Integer userId = getLoggedInUserId(); // 獲取(查出)當前用戶ID

        UserBean currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        TypeEnumBean type = typeEnumRepository.findByTypeId(typeId)
                .orElseThrow(() -> new RuntimeException("Type not found with ID: " + typeId));

        // 查找用戶現有的聊天室
        Optional<CustomerServiceChatroomBean> existingChatroom = chatroomRepository.findByUserAndType(currentUser,
                type);

        if (existingChatroom.isPresent()) {
            return existingChatroom.get();
        } else {
            // 如果不存在，則創建新的聊天室(for 用戶))
            CustomerServiceChatroomBean newChatroom = new CustomerServiceChatroomBean();
            newChatroom.setUser(currentUser);
            newChatroom.setType(type);
            newChatroom.setChatRoomName(currentUser.getGivenName() + "的" + type.getTypeName() + "聊天室"); // 可以自定義名稱
            newChatroom.setChatRoomCreateAt(LocalDateTime.now());
            return chatroomRepository.save(newChatroom);
        }
    }

    // 發送訊息
    @Transactional
    public MessageResponseDTO sendMessage(Integer chatRoomId, String content, Integer questionId, Integer typeId) {
        // 驗證聊天室是否存在
        CustomerServiceChatroomBean chatroom = chatroomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chatroom not found with ID: " + chatRoomId));

        // 驗證 typeId 與 chatroom 的 type (聊天室類型) 是否一致
        if (!chatroom.getType().getTypeId().equals(typeId)) {
            throw new IllegalArgumentException("Chatroom type ID mismatch with provided type ID.");
        }

        // 保存用戶訊息
        CustomerServiceMessageBean userMessage = new CustomerServiceMessageBean();
        userMessage.setChatRoom(chatroom);
        userMessage.setMessageContent(content);
        userMessage.setSentTime(LocalDateTime.now());
        // 根據問題模板ID，關聯問題
        if (questionId != null) {
            CustomerServiceQuestionBean question = questionRepository.findByQuestionTemplateId(questionId)
                    .orElseThrow(() -> new RuntimeException("Question template not found with ID: " + questionId));
            userMessage.setQuestion(question);
        }
        // 對於用戶訊息，admin_id 為空
        messageRepository.save(userMessage);

        // 根據用戶訊息內容或關聯的問題，生成客服回覆
        String botReplyContent;
        if (questionId != null) {
            // 如果是從預設問題發送，則直接使用預設回覆
            CustomerServiceQuestionBean question = questionRepository.findByQuestionTemplateId(questionId)
                    .orElseThrow(() -> new RuntimeException("Question not found with ID: " + questionId));
            botReplyContent = question.getReplyTemplate();
        } else {
            // 否則，根據用戶內容提供一個通用回覆(或未來調用 AI 服務)

            // 通用罐頭回復
            // botReplyContent = "感謝您的提問：「" + content + "」。我們已收到您的訊息，客服將排程盡快為您處理。";

            // demo用才加的 問題: 點數可以做什麼用？
            botReplyContent = "可至點數商城中將您辛苦累積的點數兌換成各種旅遊優惠券或精選好物呦！😆。";
            // (這裡可以整合您的 AI 邏輯或更複雜的規則判斷來生成回覆)
        }

        // 保存客服回覆
        CustomerServiceMessageBean botMessage = new CustomerServiceMessageBean();
        botMessage.setChatRoom(chatroom);
        botMessage.setMessageContent(botReplyContent);
        botMessage.setSentTime(LocalDateTime.now());

        // 從資料庫獲取一個實際存在的 AdminBean
        // 請確保 adminIdToUse 這個值在 dbo.admins 表中確實存在！
        Integer adminIdToUse = 2; // 使用實際插入到 admins 表的 admin_id // 這邊先寫死，之後要改!!!!!!!!!!!

        AdminBean admin = adminRepository.findById(adminIdToUse)
                .orElseThrow(() -> new RuntimeException(
                        "Admin not found with ID: " + adminIdToUse + ". Please ensure admin data exists."));

        botMessage.setAdmin(admin);
        messageRepository.save(botMessage);

        // 返回客服回覆的 DTO
        MessageResponseDTO responseDTO = new MessageResponseDTO();
        responseDTO.setMessageId(botMessage.getMessageId());
        responseDTO.setChatRoomId(botMessage.getChatRoom().getChatRoomId());
        responseDTO.setSender("bot");
        responseDTO.setMessageContent(botMessage.getMessageContent());
        responseDTO.setSentTime(botMessage.getSentTime());
        return responseDTO;
    }

    // 獲取聊天室的所有訊息
    @Transactional // @Transactional(readOnly = true)
    public List<MessageResponseDTO> getChatroomMessages(Integer chatRoomId) {
        CustomerServiceChatroomBean chatroom = chatroomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chatroom not found with ID: " + chatRoomId));

        List<CustomerServiceMessageBean> messages = messageRepository.findByChatRoomOrderBySentTimeAsc(chatroom);

        return messages.stream().map(msg -> {
            MessageResponseDTO dto = new MessageResponseDTO();
            dto.setMessageId(msg.getMessageId());
            dto.setChatRoomId(msg.getChatRoom().getChatRoomId());
            dto.setSender(msg.getAdmin() != null ? "bot" : "user"); // 如果 admin 不為空，則為客服訊息
            dto.setMessageContent(msg.getMessageContent());
            dto.setSentTime(msg.getSentTime());
            return dto;
        }).collect(Collectors.toList());
    }

    // 獲取所有問題模板（按類型篩選）
    @Transactional // @Transactional(readOnly = true)
    public List<QuestionTemplateDTO> getAllQuestionTemplates() {
        return questionRepository.findAll().stream().map(q -> {
            QuestionTemplateDTO dto = new QuestionTemplateDTO();
            dto.setQuestionTemplateId(q.getQuestionTemplateId());
            dto.setQuestionTemplate(q.getQuestionTemplate());
            dto.setReplyTemplate(q.getReplyTemplate());
            QuestionTemplateDTO.TypeDTO typeDto = new QuestionTemplateDTO.TypeDTO();
            typeDto.setTypeId(q.getType().getTypeId());
            typeDto.setTypeName(q.getType().getTypeName());
            dto.setType(typeDto);
            return dto;
        }).collect(Collectors.toList());
    }
}