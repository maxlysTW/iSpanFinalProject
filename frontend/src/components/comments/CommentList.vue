<template>
  <div class="comment-list">
    <!-- 除錯資訊 -->
    <!-- <div class="mb-3 p-3 bg-light rounded">
      <small class="text-muted">
        總評論數: {{ comments.length }} |
        主評論數: {{ mainComments.length }} |
        載入狀態: {{ comments.length > 0 ? '已載入' : '無資料' }} |
        當前用戶ID: {{ actualCurrentUserId }} | 用戶類型: {{ currentUser.userType }} | 用戶名: {{ currentUser.userName }}
      </small>
      <div v-if="comments.length > 0" class="mt-2">
        <small class="text-muted">
          第一筆評論: 作者ID={{ comments[0].author?.id }},
          可編輯={{ comments[0].editable }},
          可回覆={{ comments[0].replyable }},
          可檢舉={{ comments[0].reportable }},
          parentCommentId={{ comments[0].parentCommentId }}
        </small>
      </div>
      <div v-if="comments.length > 0" class="mt-1">
        <small class="text-muted">
          有 parentCommentId 的評論: {{comments.filter(c => c.parentCommentId).length}} 筆
        </small>
      </div>
      <div v-if="comments.filter(c => c.parentCommentId).length > 0" class="mt-1">
        <small class="text-muted">
          回覆評論詳情: {{comments.filter(c => c.parentCommentId).map(c => `ID:${c.id}->父ID:${c.parentCommentId}`).join(', ')
          }}
        </small>
      </div>
      <div v-if="mainCommentsWithNumber.length > 0" class="mt-1">
        <small class="text-muted">
          主留言回覆統計: {{mainCommentsWithNumber.map(c => `B${c.mainNo}: ${getRepliesFlat(c.id).length}筆回覆`).join(', ')}}
        </small>
      </div>
      <div v-if="mainCommentsWithNumber.length > 0" class="mt-1">
        <small class="text-muted">
          主留言回覆權限檢查: {{mainCommentsWithNumber.map(c => `B${c.mainNo}: replyable=${c.replyable}`).join(', ')}}
        </small>
      </div>
      <div v-if="comments.filter(c => c.parentCommentId).length > 0" class="mt-1">
        <small class="text-muted">
          回覆留言權限檢查:
          {{comments.filter(c => c.parentCommentId).map(c => `ID:${c.id}: replyable=${c.replyable}`).join(', ')}}
        </small>
      </div>
    </div> -->

    <!-- 主留言區域 -->
    <div v-for="(comment, index) in displayedComments" :key="comment.id" class="card mb-3">
      <div class="card-body">
        <div class="d-flex justify-content-between">
          <div class="d-flex">
            <img :src="getAvatar(comment)" class="rounded-circle me-2" width="40" height="40" />
            <div>
              <h6 class="mb-0">
                <p class="mt-2 d-flex align-items-center gap-2">
                  {{ getUserName(comment) }}
                  <!-- 驗證標籤 - 只有一般用戶的留言才顯示 -->
                  <span v-if="isUserComment(comment) && comment.isVerified" class="badge bg-success" title="已驗證住過">
                    <i class="bi bi-check-circle-fill me-1"></i>
                    已住過
                  </span>
                  <span v-else-if="isUserComment(comment)" class="badge bg-secondary" title="未驗證">
                    <i class="bi bi-question-circle me-1"></i>
                    未住過
                  </span>
                </p>
              </h6>
              <small class="text-muted">B{{ comment.mainNo }}・{{ formatDate(comment.createdAt) }}</small>
              <!-- 評分星星 - 刪除或檢舉時隱藏 -->
              <div v-if="comment.rating !== null && !isCommentHidden(comment)" class="d-flex align-items-center gap-1">
                <i v-for="n in 5" :key="n" class="bi" :class="getStarIcon(n, comment.rating)"
                  style="color: #ffc107; font-size: 1rem;"></i>
                <span class="text-muted ms-1">{{ comment.rating }}</span>
              </div>
            </div>
          </div>
          <div class="d-flex gap-1">
            <!-- 檢舉按鈕 - 刪除或檢舉時隱藏，自己不能檢舉自己，管理員不能檢舉 -->
            <button
              v-if="comment.author?.id !== actualCurrentUserId && !isCommentHidden(comment) && !isAdminUserForReport"
              class="btn btn-lg border-0 bg-transparent" @click="openReportModal(comment.id)" title="檢舉留言">
              <i class="bi bi-flag hover-text-danger"></i>
            </button>
            <!-- 刪除按鈕 - 只能自己刪除，刪除或檢舉時隱藏 -->
            <button class="btn btn-lg border-0 bg-transparent"
              v-if="comment.author?.id === actualCurrentUserId && !isCommentHidden(comment)"
              @click="deleteComment(comment.id)" title="刪除留言">
              <i class="bi bi-trash hover-text-danger"></i>
            </button>
            <!-- 編輯按鈕 - 只能自己編輯，刪除或檢舉時隱藏 -->
            <button class="btn btn-lg border-0 bg-transparent"
              v-if="comment.author?.id === actualCurrentUserId && !isCommentHidden(comment)"
              @click="openEditModal(comment.id)" title="編輯留言">
              <i class="bi bi-pencil hover-text-primary"></i>
            </button>
            <!-- 按讚 - 刪除或檢舉時隱藏，管理員不能按讚 -->
            <button class="btn btn-lg border-0 bg-transparent d-flex align-items-center gap-1"
              v-if="!isCommentHidden(comment) && !isAdminUserForLike" @click="toggleLike(comment.id)"
              :disabled="isAdminUserForLike"
              :title="isAdminUserForLike ? '管理員不能按讚留言' : (comment.likedByCurrentUser ? '取消按讚' : '按讚')">
              <i class="bi"
                :class="comment.likedByCurrentUser ? 'bi-hand-thumbs-up-fill text-primary' : 'bi-hand-thumbs-up'"
                style="font-size: 1.25rem;"></i>
              <span :class="[
                'like-count',
                comment.likeCount >= 3 ? 'text-warning' :
                  comment.likeCount >= 2 ? 'text-danger' : 'text-secondary'
              ]" style="font-size: 1rem; font-weight: bold;">
                {{ comment.likeCount }}
                <!-- 讚數特效 -->
                <i v-if="comment.likeCount >= 3" class="bi bi-fire ms-1 text-danger"></i>
              </span>
            </button>

            <!-- 管理員按讚提示 -->
            <div v-if="!isCommentHidden(comment) && isAdminUserForLike"
              class="d-flex align-items-center gap-1 text-muted" title="管理員不能按讚留言">
              <i class="bi bi-hand-thumbs-up" style="font-size: 1.25rem; opacity: 0.5;"></i>
              <span class="like-count" style="font-size: 1rem; font-weight: bold; opacity: 0.7;">
                {{ comment.likeCount }}
                <!-- 讚數特效 -->
                <i v-if="comment.likeCount >= 3" class="bi bi-fire ms-1 text-danger"></i>
              </span>
              <small class="text-muted">({{ currentUser.userType }})</small>
            </div>
          </div>
        </div>

        <!-- 評論內容 -->
        <p class="mt-2" :class="{ 'text-muted': isCommentHidden(comment) }">
          {{ getCommentContent(comment) }}
        </p>

        <!-- 評論圖片 - 刪除或檢舉時隱藏 -->
        <div class="d-flex flex-wrap gap-2 mt-2"
          v-if="getActiveImages(comment).length > 0 && !isCommentHidden(comment)">
          <img v-for="(img, idx) in getActiveImages(comment)" :key="`${comment.id}-${img.imageId || img.id || idx}`"
            :src="getImageUrl(img)" style="width: 80px; height: 80px; object-fit: cover; border-radius: 4px;"
            @error="handleImageError" />
        </div>

        <!-- 圖片除錯資訊 -->
        <!-- <div v-if="comment.images && comment.images.length" class="mt-1">
          <small class="text-muted">
            總圖片數量: {{ comment.images.length }} |
            活躍圖片數量: {{ getActiveImages(comment).length }} |
            第一張活躍圖片URL: {{ getActiveImages(comment).length > 0 ? getImageUrl(getActiveImages(comment)[0]) : '無' }} |
            第一張圖片資料: {{ JSON.stringify(comment.images[0]) }}
          </small>
        </div> -->

        <div class="text-end">
          <!-- 回覆按鈕：使用新的權限檢查函數 -->
          <button v-if="isCommentReplyable(comment)" class="btn btn-sm btn-outline-primary reply-btn"
            @click="openReplyModal(comment.id, `B${comment.mainNo}`)">
            回覆
          </button>
        </div>

        <!-- 回覆留言區域 -->
        <div v-for="(reply, j) in getRepliesFlat(comment.id)" :key="reply.id"
          :class="getReplyClasses(reply, comment.id)" :style="getReplyStyles(reply, comment.id)">
          <div class="d-flex justify-content-between">
            <div class="d-flex">
              <img :src="getAvatar(reply)" class="rounded-circle me-2" width="40" height="40" />
              <div>
                <h6 class="mb-0 text-secondary">
                  <p class="mt-2 d-flex align-items-center gap-2">
                    {{ getUserName(reply) }}
                    <!-- 驗證標籤 - 只有一般用戶的留言才顯示 -->
                    <span v-if="isUserComment(reply) && reply.isVerified" class="badge bg-success" title="已驗證住過">
                      <i class="bi bi-check-circle-fill me-1"></i>
                      已住過
                    </span>
                    <span v-else-if="isUserComment(reply)" class="badge bg-secondary" title="未驗證">
                      <i class="bi bi-question-circle me-1"></i>
                      未住過
                    </span>
                  </p>
                </h6>
                <small class="text-muted">{{ getReplyNumber(reply, comment.id, 0) }}・{{ formatDate(reply.createdAt)
                }}</small>
              </div>
            </div>
            <div class="d-flex gap-1">
              <!-- 檢舉按鈕 - 刪除或檢舉時隱藏，自己不能檢舉自己，管理員不能檢舉 -->
              <button
                v-if="reply.author?.id !== actualCurrentUserId && !isCommentHidden(reply) && !isAdminUserForReport"
                class="btn btn-lg border-0 bg-transparent" @click="openReportModal(reply.id)" title="檢舉留言">
                <i class="bi bi-flag hover-text-danger"></i>
              </button>
              <!-- 刪除按鈕 - 只能自己刪除，刪除或檢舉時隱藏 -->
              <button class="btn btn-lg border-0 bg-transparent"
                v-if="reply.author?.id === actualCurrentUserId && !isCommentHidden(reply)"
                @click="deleteComment(reply.id)" title="刪除留言">
                <i class="bi bi-trash hover-text-danger"></i>
              </button>
              <!-- 編輯按鈕 - 只能自己編輯，刪除或檢舉時隱藏 -->
              <button class="btn btn-lg border-0 bg-transparent"
                v-if="reply.author?.id === actualCurrentUserId && !isCommentHidden(reply)"
                @click="openEditModal(reply.id)" title="編輯留言">
                <i class="bi bi-pencil hover-text-primary"></i>
              </button>
              <!-- 按讚 - 刪除或檢舉時隱藏，管理員不能按讚 -->
              <button class="btn btn-lg border-0 bg-transparent d-flex align-items-center gap-1"
                v-if="!isCommentHidden(reply) && !isAdminUserForLike" @click="toggleLike(reply.id)"
                :disabled="isAdminUserForLike"
                :title="isAdminUserForLike ? '管理員不能按讚留言' : (reply.likedByCurrentUser ? '取消按讚' : '按讚')">
                <i class="bi"
                  :class="reply.likedByCurrentUser ? 'bi-hand-thumbs-up-fill text-primary' : 'bi-hand-thumbs-up'"
                  style="font-size: 1.25rem;"></i>
                <span :class="[
                  'like-count',
                  reply.likeCount >= 3 ? 'text-warning' :
                    reply.likeCount >= 2 ? 'text-danger' : 'text-secondary'
                ]" style="font-size: 1rem; font-weight: bold;">
                  {{ reply.likeCount }}
                  <!-- 讚數特效 -->
                  <i v-if="reply.likeCount >= 3" class="bi bi-fire ms-1 text-danger"></i>
                </span>
              </button>

              <!-- 管理員按讚提示 (回覆) -->
              <div v-if="!isCommentHidden(reply) && isAdminUserForLike"
                class="d-flex align-items-center gap-1 text-muted" title="管理員不能按讚留言">
                <i class="bi bi-hand-thumbs-up" style="font-size: 1.25rem; opacity: 0.5;"></i>
                <span class="like-count" style="font-size: 1rem; font-weight: bold; opacity: 0.7;">
                  {{ reply.likeCount }}
                  <!-- 讚數特效 -->
                  <i v-if="reply.likeCount >= 3" class="bi bi-fire ms-1 text-danger"></i>
                </span>
                <small class="text-muted">({{ currentUser.userType }})</small>
              </div>
            </div>
          </div>
          <p class="mt-2" :class="{ 'text-muted': isCommentHidden(reply) }">
            {{ getCommentContent(reply) }}
          </p>
          <div class="d-flex flex-wrap gap-2 mt-2" v-if="getActiveImages(reply).length > 0 && !isCommentHidden(reply)">
            <img v-for="(img, idx) in getActiveImages(reply)" :key="img.imageId || img.id || idx"
              :src="getImageUrl(img)" style="width: 80px; height: 80px; object-fit: cover; border-radius: 4px;"
              @error="handleImageError" />
          </div>

          <div class="text-end mt-2" v-if="isCommentReplyable(reply)">
            <button class="btn btn-sm btn-outline-primary reply-btn"
              @click="openReplyModal(reply.id, getReplyPrefix(reply, comment.id))">
              回覆
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 留言統計和展開控制 -->
  <div class="d-flex justify-content-between align-items-center mb-3">
    <div class="d-flex align-items-center gap-3">
      <h5 class="mb-0">
        <i class="bi bi-chat-dots me-2"></i>
        留言 ({{ mainCommentsWithNumber.length }})
      </h5>
      <span class="badge bg-primary">{{ displayedComments.length }} 筆顯示中</span>
    </div>

    <!-- 展開/收合按鈕 -->
    <div v-if="mainCommentsWithNumber.length > initialDisplayCount" class="d-flex gap-2">
      <button v-if="!isExpanded" @click="expandComments" class="btn btn-outline-primary btn-sm">
        <i class="bi bi-chevron-down me-1"></i>
        展開更多留言 ({{ mainCommentsWithNumber.length - initialDisplayCount }} 筆)
      </button>
      <button v-else @click="collapseComments" class="btn btn-outline-secondary btn-sm">
        <i class="bi bi-chevron-up me-1"></i>
        收合留言
      </button>
    </div>
  </div>

  <!-- Modal 彈窗區域 -->
  <!-- 編輯留言 Modal 彈窗 -->
  <Teleport to="body">
    <div v-if="showEditModal">
      <div class="modal-backdrop fade show"></div>
      <div class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5);">
        <div class="modal-dialog modal-lg modal-dialog-centered">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">編輯留言</h5>
              <button type="button" class="btn-close" @click="closeEditModal"></button>
            </div>
            <div class="modal-body">
              <CommentForm v-if="editingComment" mode="edit" :currentUserId="actualCurrentUserId.value"
                :initialContent="editingComment.content" :initialRating="editingComment.rating ?? 5"
                :initialImages="editingComment.images" :comment-id="editingCommentId" @submit="handleEditSubmit"
                @cancel="closeEditModal" @images-changed="handleImagesChanged" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>

  <!-- 檢舉留言 Modal 彈窗 -->
  <Teleport to="body">
    <div v-if="showReportCommentId !== null">
      <div class="modal-backdrop fade show"></div>
      <div class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5);">
        <div class="modal-dialog modal-sm modal-dialog-centered">
          <div class="modal-content">
            <div class="modal-header">
              <h6 class="modal-title">檢舉留言</h6>
              <button type="button" class="btn-close" @click="closeReportModal"></button>
            </div>
            <div class="modal-body">
              <ReportReason :commentId="showReportCommentId" :currentUserId="actualCurrentUserId.value"
                @close="closeReportModal" @reported="handleCommentReported" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>

  <!-- 回覆留言的 Modal 彈窗 -->
  <Teleport to="body">
    <div v-if="replyModalCommentId !== null">
      <div class="modal-backdrop fade show"></div>
      <div class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5);">
        <div class="modal-dialog modal-lg modal-dialog-centered">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">回覆留言</h5>
              <button type="button" class="btn-close" @click="closeReplyModal"></button>
            </div>
            <div class="modal-body">
              <CommentForm mode="reply" :parentId="replyModalCommentId" :currentUserId="actualCurrentUserId.value"
                :initialContent="replyPrefix" @submit="handleReplySubmit" @cancel="closeReplyModal" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
  import { ref, computed, watch, onUnmounted, onMounted, nextTick } from "vue";
  import { commentAPI } from '@/services/api'
  import CommentForm from "./CommentForm.vue";
  import ReportReason from "./ReportReason.vue";
  import { useCurrentUser } from '@/composables/useCurrentUser'
  import { useCommentPermissions } from '@/composables/useCommentPermissions'
  import { useCommentErrorHandler } from '@/composables/useCommentErrorHandler'

  // 安全的圖片深拷貝函數 - 避免遞迴巢狀結構
  function deepCloneImages(images) {
    try {
      if (!images || !Array.isArray(images)) {
        return [];
      }

      return images.map((img, index) => {
        try {
          if (!img || typeof img !== 'object') {
            return img;
          }

          // 排除無法序列化的屬性
          const { file, ...safeImg } = img;

          // 創建安全的拷貝，只保留必要欄位
          const clonedImg = {
            id: safeImg.id || safeImg.imageId || `temp-${index}`,
            name: safeImg.name || `圖片 ${index + 1}`,
            previewUrl: safeImg.previewUrl || safeImg.commentImageUrl || safeImg.imageUrl,
            size: safeImg.size || 0,
            imageId: safeImg.imageId,
            commentImageUrl: safeImg.commentImageUrl,
            imageUrl: safeImg.imageUrl,
            isActive: safeImg.isActive !== undefined ? safeImg.isActive : true,
            isDeleted: safeImg.isDeleted || false,
            sortOrder: safeImg.sortOrder || index,
            mimeType: safeImg.mimeType,
            processing: safeImg.processing || false,
            error: safeImg.error || null,
            progress: safeImg.progress || 100,
            deleteStatus: safeImg.deleteStatus,
            isNew: safeImg.isNew || false
          };

          // 如果有 file 屬性，保留引用（不進行深拷貝）
          if (file) {
            clonedImg.file = file;
          }

          // 如果有 originalImg 屬性，只保留必要欄位避免遞迴
          if (safeImg.originalImg && typeof safeImg.originalImg === 'object') {
            const {
              id, name, previewUrl, size, imageId, commentImageUrl,
              imageUrl, isActive, isDeleted, sortOrder, mimeType,
              processing, error, progress, deleteStatus
            } = safeImg.originalImg;

            clonedImg.originalImg = {
              id, name, previewUrl, size, imageId, commentImageUrl,
              imageUrl, isActive, isDeleted, sortOrder, mimeType,
              processing, error, progress, deleteStatus
            };
          }

          return clonedImg;
        } catch (error) {
          console.error(`❌ 處理圖片 ${index} 時發生錯誤:`, error);
          return null;
        }
      }).filter(img => img !== null); // 過濾掉 null 值
    } catch (error) {
      console.error('❌ deepCloneImages 函數發生錯誤:', error);
      return [];
    }
  }

  const editingCommentId = ref(null);
  const showEditModal = ref(false);
  const showReportCommentId = ref(null);
  const replyingTo = ref(null);
  const replyPrefix = ref('');
  const props = defineProps({
    comments: {
      type: Array,
      required: true
    },
    currentUserId: {
      type: Number,
      required: true
    }
  });

  // 使用統一的用戶資訊，確保 currentUserId 是正確的
  const actualCurrentUserId = computed(() => {
    // 如果 props.currentUserId 存在且有效，使用它
    if (props.currentUserId && props.currentUserId > 0) {
      return props.currentUserId
    }
    // 否則使用 useCurrentUser 中的 userId
    return currentUser.value.userId
  })

  const emit = defineEmits(['submit', 'delete', 'update']);

  const replyModalCommentId = ref(null);

  // 把 editingComment 改成 ref，別再用 computed
  const editingComment = ref(null)

  // 監聽 editingCommentId 變化，更新 editingComment
  watch(editingCommentId, id => {
    editingComment.value = props.comments.find(c => c.id === id) ?? null
  })

  // 由於後端回傳的是扁平結構，我們需要重新組織資料
  const mainComments = computed(() => {
    // 找出所有主評論（沒有 parentCommentId 的評論）
    return props.comments.filter((c) => !c.parentCommentId)
  });

  const mainCommentsWithNumber = computed(() => {
    const mainComments = props.comments.filter(c => !c.parentCommentId)
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()) // 按時間排序，新的在前

    return mainComments.map((comment, idx) => ({
      ...comment,
      mainNo: mainComments.length - idx // 新的留言獲得較大的標號
    }))
  });

  // ====== 展開留言狀態與顯示控制 ======
  const initialDisplayCount = 2; // 預設顯示幾筆
  const isExpanded = ref(false);

  const displayedComments = computed(() => {
    if (isExpanded.value) {
      return mainCommentsWithNumber.value;
    } else {
      return mainCommentsWithNumber.value.slice(0, initialDisplayCount);
    }
  });

  function expandComments() {
    isExpanded.value = true;
  }
  function collapseComments() {
    isExpanded.value = false;
  }

  // 取得共用用戶資訊與權限
  const { currentUser, isAdmin, isVendor, isAdminOrVendor } = useCurrentUser()

  // 檢查評論是否可回覆（所有用戶都可以回覆）
  function isCommentReplyable(comment) {
    // 如果評論未刪除/檢舉，則允許回覆
    if (!isCommentHidden(comment)) return true

    return false
  }

  // 檢查是否為管理員（只有管理員不能按讚）
  const isAdminUserForLike = isAdmin
  // 檢查是否為管理員（只有管理員不能檢舉）
  const isAdminUserForReport = isAdmin

  // 取得用戶頭像
  function getUserAvatar(comment) {
    if (comment.author?.avatarUrl) {
      return comment.author.avatarUrl
    }
    return `https://i.pravatar.cc/150?u=${comment.author?.id || comment.id}`
  }

  // 取得評論內容 - 處理刪除和檢舉狀態
  function getCommentContent(comment) {
    // 使用者刪除 (isActive = false, deleteStatus = 0)
    if (comment.deleteStatus === 0) {
      return '作者刪除留言'
    }
    // 系統下架/檢舉 (isActive = false, deleteStatus = 1)
    if (!comment.isActive && comment.deleteStatus === 1) {
      return '留言因違反規定已被移除'
    }
    return comment.content
  }

  // 取得用戶名稱 - 處理刪除和檢舉狀態
  function getUserName(comment) {
    // 使用者刪除 (isActive = false, deleteStatus = 0)
    if (comment.deleteStatus === 0) {
      return '作者刪除留言'
    }
    // 系統下架/檢舉 (isActive = false, deleteStatus = 1)
    if (!comment.isActive && comment.deleteStatus === 1) {
      return '留言因違反規定已被移除'
    }
    return comment.userName || comment.author?.username || '未知用戶'
  }

  // 取得頭像 - 處理刪除和檢舉狀態
  function getAvatar(comment) {
    // 使用者刪除或系統下架時顯示灰色頭像
    if (comment.deleteStatus === 0 || (!comment.isActive && comment.deleteStatus === 1)) {
      // 回傳灰色頭像
      return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMjAiIGN5PSIyMCIgcj0iMjAiIGZpbGw9IiM5QzlDOUMiLz4KPHBhdGggZD0iTTIwIDEwQzE2LjEzIDEwIDEzIDEzLjEzIDEzIDE3QzEzIDIwLjg3IDE2LjEzIDI0IDIwIDI0QzIzLjg3IDI0IDI3IDIwLjg3IDI3IDE3QzI3IDEzLjEzIDIzLjg3IDEwIDIwIDEwWiIgZmlsbD0iI0Y1RjVGNSIvPgo8cGF0aCBkPSJNMTAgMzBDMTAgMjUuNTggMTQuNTggMjEgMjAgMjFDMjUuNDIgMjEgMzAgMjUuNTggMzAgMzBIMFYzMFoiIGZpbGw9IiNGNUY1RjUiLz4KPC9zdmc+';
    }
    return getUserAvatar(comment)
  }

  // 檢查評論是否被刪除或檢舉
  function isCommentHidden(comment) {
    // 使用者刪除 (deleteStatus = 0) 或 系統下架/檢舉 (isActive = false, deleteStatus = 1)
    return comment.deleteStatus === 0 || (!comment.isActive && comment.deleteStatus === 1)
  }

  // 檢查是否為一般用戶的留言
  function isUserComment(comment) {
    return comment.author?.userType === 'USER'
  }

  function getRepliesFlat(parentId) {
    // 找出所有回覆該主留言的評論（包括巢狀回覆）
    const allReplies = props.comments.filter(c => {
      // 檢查是否屬於這個主留言的回覆（遞迴檢查）
      let currentParentId = c.parentCommentId;
      while (currentParentId) {
        if (currentParentId === parentId) {
          return true;
        }
        const parentComment = props.comments.find(p => p.id === currentParentId);
        if (!parentComment) break;
        currentParentId = parentComment.parentCommentId;
      }
      return false;
    });

    // 按照在主留言下的順序排序（根據 ID 或時間）
    allReplies.sort((a, b) => a.id - b.id);

    // console.log(`🔍 主留言 ${parentId} 的所有回覆:`, allReplies.map(r => `ID:${r.id}, parentCommentId:${r.parentCommentId}`))
    return allReplies;
  }

  function openEditModal(id) {
    editingCommentId.value = id;
    showEditModal.value = true;

    // 設置 body 狀態
    document.body.classList.add('modal-open');
  }

  function addReply(payload) {
    emit("submit", payload);
    replyingTo.value = null;
  };

  function deleteComment(commentId) {
    // 發送刪除事件到父組件
    emit("delete", commentId);
  }

  function formatDate(dateStr) {
    const d = new Date(dateStr)
    return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
  };

  function getStarIcon(n, rating) {
    if (rating == null) return 'bi-star'
    if (rating >= n) return 'bi-star-fill'
    return 'bi-star'
  };

  function closeEditModal() {
    showEditModal.value = false;
    editingCommentId.value = null;

    // 等 Teleport 把節點移除再清 body 狀態，避免閃動
    nextTick(() => document.body.classList.remove('modal-open'));
  }

  function updateComment(commentId, payload) {
    // 發送更新事件到父組件
    emit("update", commentId, payload);
    closeEditModal();
  }

  // 處理編輯提交
  function handleEditSubmit(payload) {
    // 先保存 commentId，避免 closeEditModal 後被清空
    const commentId = editingCommentId.value;

    // 安全起見先把 modal 關掉，成功失敗都不會卡住畫面
    closeEditModal();

    // 把真正更新交給父層
    emit('update', commentId, payload);
  }

  // 監聽留言狀態變化事件
  onMounted(() => {
    window.addEventListener('commentStatusChanged', handleCommentStatusChange);
  });

  onUnmounted(() => {
    window.removeEventListener('commentStatusChanged', handleCommentStatusChange);
  });

  // 處理留言狀態變化
  function handleCommentStatusChange(event) {
    const { commentId, comment } = event.detail;
    console.log(`🔄 收到留言 ${commentId} 狀態變化:`, comment);

    // 更新本地留言狀態
    updateCommentStatus(commentId, comment);
  }

  // 更新留言狀態
  function updateCommentStatus(commentId, newComment) {
    // 在 props.comments 中找到對應的留言並更新
    const commentIndex = props.comments.findIndex(c => c.id === commentId);

    if (commentIndex !== -1) {
      // 更新留言狀態
      Object.assign(props.comments[commentIndex], newComment);

      console.log(`✅ 留言 ${commentId} 狀態已更新:`, {
        isActive: newComment.isActive,
        deleteStatus: newComment.deleteStatus,
        statusMessage: newComment.statusMessage
      });

      // 如果留言已被隱藏或刪除，顯示提示
      if (!newComment.isActive || newComment.deleteStatus === 0) {
        showStatusChangeNotification(commentId, newComment);
      }
    }
  }

  // 顯示狀態變化通知
  function showStatusChangeNotification(commentId, comment) {
    let message = '';

    if (comment.deleteStatus === 0) {
      message = '您檢舉的留言已被刪除';
    } else if (!comment.isActive) {
      message = '您檢舉的留言已被隱藏';
    }

    if (message) {
      // 使用 Toast 或 SweetAlert 顯示通知
      console.log(`📢 ${message}: 留言 ${commentId}`);

      // 這裡可以整合 SweetAlert 或其他通知組件
      // Swal.fire({
      //   icon: 'success',
      //   title: '檢舉處理完成',
      //   text: message,
      //   timer: 3000,
      //   showConfirmButton: false
      // });
    }
  }

  // 處理圖片狀態變化 - 只在真正需要時更新，避免無限迴圈
  function handleImagesChanged(commentId, newImages) {
    console.log('🔄 處理圖片狀態變化:', {
      commentId,
      newImagesCount: newImages?.length || 0,
      newImages: newImages?.map(img => ({
        id: img.id,
        imageId: img.imageId,
        isActive: img.isActive,
        isDeleted: img.isDeleted,
        commentImageUrl: img.commentImageUrl,
        imageUrl: img.imageUrl,
        previewUrl: img.previewUrl
      }))
    });

    // 找到對應的評論
    const comment = props.comments.find(c => c.id === commentId);
    if (comment) {
      // 檢查是否為相同的引用，避免無限迴圈
      if (newImages === comment.images) {
        console.log('🔄 檢測到相同引用，跳過更新以避免無限迴圈');
        return;
      }

      // ✅ 檢查是否真的需要更新
      const currentImagesStr = JSON.stringify(comment.images || []);
      const newImagesStr = JSON.stringify(newImages || []);
      const needsUpdate = currentImagesStr !== newImagesStr;

      if (needsUpdate) {
        try {
          // ✅ 過濾掉已刪除的圖片
          const cleanImages = newImages.filter(img => !img.isDeleted);
          console.log(`✅ 過濾後保留 ${cleanImages.length} 張圖片（原本 ${newImages.length} 張）`);

          // 使用深拷貝，避免共享引用
          comment.images = deepCloneImages(cleanImages);

          console.log('✅ 評論圖片狀態已更新:', {
            commentId,
            totalImages: comment.images.length,
            activeImages: comment.images.filter(img => img.isActive !== false).length,
            deletedImages: comment.images.filter(img => img.isActive === false).length,
            sampleImage: comment.images[0] ? {
              id: comment.images[0].id,
              imageId: comment.images[0].imageId,
              imageUrl: comment.images[0].imageUrl,
              commentImageUrl: comment.images[0].commentImageUrl,
              previewUrl: comment.images[0].previewUrl
            } : null
          });
        } catch (error) {
          console.error('❌ 更新評論圖片時發生錯誤:', error);
          // 如果處理失敗，至少保持原有狀態
          comment.images = [];
        }
      } else {
        console.log('🔄 跳過更新：圖片資料沒有變化');
      }
    } else {
      console.warn('⚠️ 找不到對應的評論:', commentId);
    }
  }

  function openReplyModal(id, prefix) {
    replyModalCommentId.value = id;
    replyPrefix.value = prefix;

    // 設置 body 狀態
    document.body.classList.add('modal-open');
  }

  function closeReplyModal() {
    replyModalCommentId.value = null;
    replyPrefix.value = '';

    // 等 Teleport 把節點移除再清 body 狀態，避免閃動
    nextTick(() => document.body.classList.remove('modal-open'));
  }

  // 處理檢舉 Modal 關閉
  function closeReportModal() {
    showReportCommentId.value = null;

    // 等 Teleport 把節點移除再清 body 狀態，避免閃動
    nextTick(() => document.body.classList.remove('modal-open'));
  }

  // 開啟檢舉 Modal
  function openReportModal(commentId) {
    showReportCommentId.value = commentId;

    // 設置 body 狀態
    document.body.classList.add('modal-open');
  }

  function handleReplySubmit(payload) {
    console.log('🔄 回覆留言 payload:', payload);
    console.log('🔄 回覆目標 ID:', replyModalCommentId.value);
    console.log('🔄 parentCommentId:', payload.parentCommentId);
    emit('submit', payload);
    closeReplyModal();
  }

  async function toggleLike(commentId) {
    const comment = props.comments.find(c => c.id === commentId)
    if (!comment) return

    // 檢查是否為管理員（只有管理員不能按讚）
    if (isAdmin.value) {
      Swal.fire({
        icon: 'warning',
        title: '權限不足',
        text: '管理員不能按讚留言',
        confirmButtonText: '確定'
      })
      return
    }

    // 樂觀更新 - 立即更新 UI
    const originalLiked = comment.likedByCurrentUser
    const originalCount = comment.likeCount

    comment.likedByCurrentUser = !originalLiked
    comment.likeCount = originalLiked ? (originalCount - 1) : (originalCount + 1)

    try {
      const userId = actualCurrentUserId.value; // 獲取實際的數值
      console.log(`🔄 切換按讚狀態: 留言 ID ${commentId}, 用戶 ID ${userId}`)

      const response = await commentAPI.toggleLike(commentId, userId)

      // 更新實際的按讚狀態
      if (comment) {
        comment.likedByCurrentUser = response.data.likedByCurrentUser
        comment.likeCount = response.data.likeCount

        console.log(`✅ 按讚狀態更新:`, {
          commentId,
          likedByCurrentUser: response.data.likedByCurrentUser,
          likeCount: response.data.likeCount,
          message: response.data.message
        })
      }
    } catch (err) {
      console.error('❌ 按讚操作失敗:', err)

      // 恢復原始狀態
      comment.likedByCurrentUser = originalLiked
      comment.likeCount = originalCount

      // 顯示錯誤訊息
      const errorMessage = err.response?.data?.message || '按讚操作失敗，請稍後再試'
      Swal.fire({
        icon: 'error',
        title: '操作失敗',
        text: errorMessage,
        confirmButtonText: '確定'
      })
    }
  }

  // 載入評論的按讚狀態
  async function loadLikeStatus(commentId) {
    const comment = props.comments.find(c => c.id === commentId)
    if (!comment) return

    try {
      const userId = actualCurrentUserId.value; // 獲取實際的數值
      const response = await commentAPI.getLikeStatus(commentId, userId)

      if (comment) {
        comment.likedByCurrentUser = response.data.likedByCurrentUser
        comment.likeCount = response.data.likeCount

        console.log(`📊 載入按讚狀態: 留言 ${commentId}`, {
          likedByCurrentUser: response.data.likedByCurrentUser,
          likeCount: response.data.likeCount
        })
      }
    } catch (err) {
      console.error(`❌ 載入按讚狀態失敗 (留言 ${commentId}):`, err)
    }
  }

  // 批量載入所有評論的按讚狀態
  async function loadAllLikeStatus() {
    console.log('🔄 開始載入所有評論的按讚狀態...')

    // 限制並發數量，避免過度並發請求
    const batchSize = 5
    const comments = props.comments
    const batches = []

    for (let i = 0; i < comments.length; i += batchSize) {
      batches.push(comments.slice(i, i + batchSize))
    }

    try {
      for (const batch of batches) {
        const promises = batch.map(comment => loadLikeStatus(comment.id))
        await Promise.all(promises)

        // 添加小延遲避免過度並發
        if (batches.length > 1) {
          await new Promise(resolve => setTimeout(resolve, 100))
        }
      }

      console.log('✅ 所有評論的按讚狀態載入完成')
    } catch (err) {
      console.error('❌ 批量載入按讚狀態失敗:', err)
    }
  }

  function handleImageError(event) {
    const img = event.target;
    console.error('🖼️ 圖片載入失敗:', {
      src: img.src,
      naturalWidth: img.naturalWidth,
      naturalHeight: img.naturalHeight
    });

    // 如果是 blob URL 失敗，顯示預設圖片
    if (img.src.startsWith('blob:')) {
      console.warn('⚠️ Blob URL 載入失敗，顯示預設圖片');
      img.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iODAiIHZpZXdCb3g9IjAgMCA4MCA4MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjgwIiBmaWxsPSIjRjVGNUY1Ii8+CjxwYXRoIGQ9Ik0yMCAyMEg2MFY2MEgyMFYyMFoiIGZpbGw9IiNEN0Q3RDciLz4KPHBhdGggZD0iTTI1IDI1SDU1VjU1SDI1VjI1WiIgZmlsbD0iI0Y1RjVGNSIvPgo8L3N2Zz4K';
    }
  }

  // 取得活躍的圖片（過濾已刪除的圖片）
  function getActiveImages(comment) {
    if (!comment.images || !Array.isArray(comment.images)) {
      return [];
    }

    // 過濾掉已刪除的圖片 - 即時計算，不使用快取
    const activeImages = comment.images.filter(img => {
      // 檢查 isActive 狀態
      if (img.isActive === false) {
        return false;
      }

      // 檢查 isDeleted 標記
      if (img.isDeleted === true) {
        return false;
      }

      // 檢查 deleteStatus（如果存在）
      if (img.deleteStatus === 0) {
        return false;
      }

      return true;
    });

    // 減少日誌輸出，只在有變化時輸出
    const shouldLog = comment.id === 14; // 只對特定評論輸出日誌
    if (shouldLog) {
      console.log(`🖼️ 評論 ${comment.id} 的活躍圖片:`, {
        totalImages: comment.images.length,
        activeImages: activeImages.length,
        filteredImages: activeImages.map(img => ({
          id: img.id,
          imageId: img.imageId,
          isActive: img.isActive,
          isDeleted: img.isDeleted,
          deleteStatus: img.deleteStatus
        }))
      });
    }

    return activeImages;
  }

  function getImageUrl(img) {
    const fallbackImage =
      'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iODAiIHZpZXdCb3g9IjAgMCA4MCA4MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjgwIiBmaWxsPSIjRjVGNUY1Ii8+CjxwYXRoIGQ9Ik0yMCAyMEg2MFY2MEgyMFYyMFoiIGZpbGw9IiNEN0Q3RDciLz4KPHBhdGggZD0iTTI1IDI1SDU1VjU1SDI1VjI1WiIgZmlsbD0iI0Y1RjVGNSIvPgo8L3N2Zz4K';

    let url = null;

    // ✅ 優先取得圖片 URL
    if (img?.commentImageUrl) {
      url = img.commentImageUrl;
    } else if (img?.imageUrl) {
      url = img.imageUrl;
    } else {
      console.error('❌ 找不到圖片 URL:', img);
      return fallbackImage;
    }

    // ✅ blob URL → 回傳預設圖
    if (url.startsWith('blob:')) {
      console.warn('⚠️ 檢測到 blob URL，可能已過期:', url);
      return fallbackImage;
    }

    // ✅ http://192.168.1.102:8080/uploads/... → /api/uploads/...
    const prefix = 'https://192.168.36.96:8080/uploads/';
    if (url.startsWith(prefix)) {
      url = url.replace(prefix, '/api/uploads/');
    }

    // ✅ 若為 /uploads/... → 加上 /api 前綴（避免遺漏）
    if (url.startsWith('/uploads/')) {
      url = '/api' + url;
    }

    return url;
  }

  function handleCommentReported(commentId) {
    console.log('🎉 評論被檢舉成功:', commentId);

    // 找到被檢舉的評論
    const reportedComment = props.comments.find(c => c.id === commentId);
    if (reportedComment) {
      // ✅ 只標記為已檢舉，不立即隱藏留言
      // 留言的顯示/隱藏應由後端審核後決定
      reportedComment.reported = true; // 可選：標記為已檢舉狀態

      console.log('✅ 評論已標記為已檢舉，等待管理員審核:', reportedComment);
    }

    // 關閉檢舉 Modal
    closeReportModal();
  }

  // 計算回覆的層級（距離主留言的深度）
  function getReplyLevel(reply, mainCommentId) {
    let level = 0;
    let currentParentId = reply.parentCommentId;

    while (currentParentId && currentParentId !== mainCommentId) {
      level++;
      const parentComment = props.comments.find(c => c.id === currentParentId);
      if (!parentComment) break;
      currentParentId = parentComment.parentCommentId;
    }

    return level;
  }

  // 取得回覆的 CSS 類別
  function getReplyClasses(reply, mainCommentId) {
    const baseClasses = 'my-2 ps-3 border-start border rounded';
    return `ms-4 ${baseClasses}`;
  }

  // 取得回覆的樣式
  function getReplyStyles(reply, mainCommentId) {
    const level = getReplyLevel(reply, mainCommentId);
    const marginLeft = 16 + (level * 16); // 16px = 1rem, 每層增加 16px

    return `border-color: #777777; margin-left: ${marginLeft}px;`;
  }

  // 取得回覆的編號
  function getReplyNumber(reply, mainCommentId, index) {
    const mainComment = props.comments.find(c => c.id === mainCommentId);

    if (!mainComment) return `B?-${index + 1}`;

    // 找到主留言在 mainCommentsWithNumber 中的索引
    const mainCommentIndex = mainCommentsWithNumber.value.findIndex(c => c.id === mainCommentId);
    const mainNo = mainCommentIndex >= 0 ? mainCommentsWithNumber.value[mainCommentIndex].mainNo : '?';

    // 所有回覆都按照在主留言下的順序編號，不考慮層級
    const allReplies = getRepliesFlat(mainCommentId);
    const replyIndex = allReplies.findIndex(r => r.id === reply.id);

    return `B${mainNo}-${replyIndex + 1}`;
  }

  function getReplyPrefix(reply, mainCommentId) {
    // 根據被回覆的留言來決定前綴
    // reply 是要被回覆的留言，我們需要根據它的編號來決定前綴

    // 找到被回覆留言在同一個主留言下的編號
    const allReplies = getRepliesFlat(mainCommentId);
    const replyIndex = allReplies.findIndex(r => r.id === reply.id);

    if (replyIndex >= 0) {
      const mainComment = props.comments.find(c => c.id === mainCommentId);
      if (!mainComment) return 'B? ';

      const mainCommentIndex = mainCommentsWithNumber.value.findIndex(c => c.id === mainCommentId);
      const mainNo = mainCommentIndex >= 0 ? mainCommentsWithNumber.value[mainCommentIndex].mainNo : '?';
      return `B${mainNo}-${replyIndex + 1} `;
    }

    return 'B? ';
  }

  // 組件卸載時清理 Modal 狀態
  onUnmounted(() => {
    // 清理 body 狀態
    document.body.classList.remove('modal-open');
    console.log('🧹 CommentList 組件已清理');
  });
</script>

<style scoped>
  .badge.bg-success {
    font-size: 0.75rem;
    vertical-align: middle;
  }

  .hover-text-danger:hover {
    color: var(--danger-color) !important;
  }

  .hover-text-primary:hover {
    color: var(--bs-success) !important;
  }

  .reply-btn {
    margin-bottom: 10px;
    margin-right: 10px;
  }
</style>