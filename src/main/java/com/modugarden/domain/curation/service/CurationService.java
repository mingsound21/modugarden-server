package com.modugarden.domain.curation.service;

import com.modugarden.common.error.enums.ErrorMessage;
import com.modugarden.common.error.exception.custom.BusinessException;
import com.modugarden.common.s3.FileService;
import com.modugarden.domain.category.entity.InterestCategory;
import com.modugarden.domain.auth.entity.ModugardenUser;
import com.modugarden.domain.category.repository.InterestCategoryRepository;
import com.modugarden.domain.curation.dto.request.CurationCreateRequestDto;
import com.modugarden.domain.curation.dto.response.*;
import com.modugarden.domain.curation.entity.Curation;
import com.modugarden.domain.curation.repository.CurationRepository;
import com.modugarden.domain.follow.repository.FollowRepository;
import com.modugarden.domain.like.entity.LikeCuration;
import com.modugarden.domain.like.repository.LikeCurationRepository;
import com.modugarden.domain.report.repository.ReportCurationRepository;
import com.modugarden.domain.storage.entity.CurationStorage;
import com.modugarden.domain.storage.repository.CurationStorageRepository;
import com.modugarden.domain.user.entity.User;
import com.modugarden.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CurationService {

    private final CurationRepository curationRepository;
    private final UserRepository userRepository;
    private final LikeCurationRepository likeCurationRepository;
    private final FileService fileService;
    private final CurationStorageRepository curationStorageRepository;
    private final InterestCategoryRepository interestCategoryRepository;
    private final FollowRepository followRepository;
    private final ReportCurationRepository reportCurationRepository;
    //큐레이션 생성
    @Transactional
    public CurationCreateResponseDto createCuration(CurationCreateRequestDto createRequestDto, MultipartFile file, ModugardenUser user) throws IOException {

        if (file.isEmpty())
            throw new IOException(new BusinessException(ErrorMessage.WRONG_CURATION_FILE));

        InterestCategory interestCategory = interestCategoryRepository.findByCategory(createRequestDto.getCategory()).get();
        String profileImageUrl = fileService.uploadFile(file, user.getUserId(), "curationImage");

        Curation curation = Curation.builder()
                .title(createRequestDto.getTitle())
                .link(createRequestDto.getLink())
                .previewImage(profileImageUrl)
                .user(user.getUser())
                .likeNum(0L)
                .category(interestCategory)
                .build();

        return new CurationCreateResponseDto(curationRepository.save(curation).getId());
    }

    //큐레이션 좋아요 달기
    @Transactional
    public CurationLikeResponseDto createLikeCuration(Long curation_id, ModugardenUser user) {
        Curation curation = curationRepository.findById(curation_id).orElseThrow(() -> new BusinessException(ErrorMessage.WRONG_CURATION));

        if (likeCurationRepository.findByUserAndCuration(user.getUser(), curation).isEmpty()) {
            curation.addLike();
            likeCurationRepository.save(new LikeCuration(user.getUser(), curation));
        }
        return new CurationLikeResponseDto(curation.getId(), curation.getLikeNum());
    }

    //큐레이션 보관
    public CurationStorageResponseDto storeCuration(ModugardenUser user, Long curation_id) {
        Curation curation = curationRepository.findById(curation_id).orElseThrow(() -> new BusinessException(ErrorMessage.WRONG_CURATION));
        if(curationStorageRepository.findByUserAndCuration(user.getUser(),curation).isPresent())
            throw new BusinessException(ErrorMessage.WRONG_CURATION_STORAGE);

        CurationStorage curationStorage = new CurationStorage(user.getUser(), curation);
        curationStorageRepository.save(curationStorage);
        return new CurationStorageResponseDto(curationStorage.getUser().getId(),curationStorage.getCuration().getId());
    }

    //큐레이션 하나 조회 api
    public CurationGetResponseDto getCuration(long id,ModugardenUser user) {
        Curation curation = curationRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorMessage.WRONG_CURATION));
        return new CurationGetResponseDto(curation,likeCurationRepository.findByUserAndCuration(user.getUser(), curation).isPresent(),curationStorageRepository.findByUserAndCuration(user.getUser(), curation).isPresent());
    }

    //회원 큐레이션 조회
    public Slice<CurationUserGetResponseDto> getUserCuration(long user_id, Pageable pageable) {
        Slice<Curation> userCurationList = curationRepository.findAllByUser_IdOrderByCreatedDateDesc(user_id, pageable);

        if (userCurationList.isEmpty())
            throw new BusinessException(ErrorMessage.WRONG_CURATION_LIST);

        return userCurationList
                .map(CurationUserGetResponseDto::new);
    }

    //제목별 큐레이션 검색
    public Slice<CurationSearchResponseDto> searchCuration(String title, Pageable pageable, Long userId) {
        Slice<Curation> SearchCurationList = curationRepository.querySearchCuration('%' + title + '%', pageable, userId);
        if (SearchCurationList.isEmpty())
            throw new BusinessException(ErrorMessage.WRONG_CURATION_LIST);

        return SearchCurationList.map(CurationSearchResponseDto::new);
    }

    //카테고리,날짜별 큐레이션 조회
    public Slice<CurationSearchResponseDto> getFeed(User user, String category, Pageable pageable) {
        InterestCategory interestCategory = interestCategoryRepository.findByCategory(category).get();
        Slice<Curation> getFeedCurationList = curationRepository.querySearchCurationByCategory(user.getId(),interestCategory, pageable);

        if (getFeedCurationList.isEmpty())
            throw new BusinessException(ErrorMessage.WRONG_CURATION_LIST);

        return getFeedCurationList.map(CurationSearchResponseDto::new);
    }

    //큐레이션 좋아요 개수 조회
    public CurationLikeResponseDto getLikeCuration(long id) {
        Curation curation = curationRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorMessage.WRONG_CURATION));
        return new CurationLikeResponseDto(curation.getId(), curation.getLikeNum());
    }

    //내 프로필 큐레이션 조회 api
    public Slice<CurationMyProfileGetResponseDto> getMyCuration(long user_id, Pageable pageable) {
        Slice<Curation> myCurationList = curationRepository.findAllByUser_IdOrderByCreatedDateDesc(user_id, pageable);
        if (myCurationList.isEmpty())
            throw new BusinessException(ErrorMessage.WRONG_CURATION_LIST);

        return myCurationList
                .map(CurationMyProfileGetResponseDto::new);
    }

    //내 프로필 큐레이션 좋아요 조회 api
    public CurationGetMyLikeResponseDto getMyLikeCuration(long curation_id,ModugardenUser user) {
        Curation curation = curationRepository.findById(curation_id).orElseThrow(() -> new BusinessException(ErrorMessage.WRONG_CURATION));

        if(likeCurationRepository.findByUserAndCuration(user.getUser(), curation).isPresent())
            return new CurationGetMyLikeResponseDto(user.getUserId(),curation.getId(), true);

        return new CurationGetMyLikeResponseDto(user.getUserId(),curation.getId(), false);
    }
    
    //내 프로필 저장한 큐레이션 조회
    public Slice<CurationGetStorageResponseDto> getStorageCuration(long user_id, Pageable pageable) {
        Slice<Curation> curationStorageList = curationRepository.QueryfindAllByUser_Id(user_id, pageable);

        if (curationStorageList.isEmpty())
            throw new BusinessException(ErrorMessage.WRONG_CURATION_LIST);

        return curationStorageList.map(
                CurationGetStorageResponseDto::new);
    }

    //내 프로필 큐레이션 보관 여부 조회 api
    public CurationGetMyStorageResponseDto getMyStorageCuration(long curation_id,ModugardenUser user) {
        Curation curation = curationRepository.findById(curation_id).orElseThrow(() -> new BusinessException(ErrorMessage.WRONG_CURATION));

        if(curationStorageRepository.findByUserAndCuration(user.getUser(), curation).isPresent())
            return new CurationGetMyStorageResponseDto(user.getUserId(),curation.getId(), true);

        return new CurationGetMyStorageResponseDto(user.getUserId(),curation.getId(), false);
    }

    //큐레이션 삭제
    @Transactional
    public CurationDeleteResponseDto delete(long id, User user) {
        Curation curation = curationRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorMessage.WRONG_CURATION_DELETE));
        fileService.deleteFile(curation.getPreviewImage()); // s3에서 이미지 삭제
        if (curation.getUser().getId().equals(user.getId())) {
            // 보관 모두 삭제
            curationStorageRepository.deleteAllByCuration(curation);
            // 좋아요 모두 삭제
            likeCurationRepository.deleteAllByCuration(curation);
            // 신고 모두 삭제
            reportCurationRepository.deleteAllByReportCuration(curation);
            curationRepository.delete(curation);
        }
        else
            throw new BusinessException(ErrorMessage.WRONG_CURATION_DELETE);

        return new CurationDeleteResponseDto(curation.getId());
    }

    //큐레이션 좋아요 취소
    @Transactional
    public CurationLikeResponseDto createUnlikeCuration(Long curation_id, ModugardenUser user) {
        Curation curation = curationRepository.findById(curation_id).orElseThrow(() -> new BusinessException(ErrorMessage.WRONG_CURATION));
        User users = userRepository.findById(user.getUserId()).orElseThrow(() -> new BusinessException(ErrorMessage.USER_NOT_FOUND));

        likeCurationRepository.findByUserAndCuration(users, curation)
                .ifPresent(it -> {
                    curation.delLike();
                    likeCurationRepository.delete(it);
                });

        return new CurationLikeResponseDto(curation.getId(), curation.getLikeNum());
    }

    //큐레이션 보관 취소
    public CurationStorageResponseDto storeCancelCuration(ModugardenUser user, Long curation_id) {
        Curation curation = curationRepository.findById(curation_id).orElseThrow(() -> new BusinessException(ErrorMessage.WRONG_CURATION));
        curationStorageRepository.findByUserAndCuration(user.getUser(),curation).ifPresent(
                curationStorageRepository::delete
        );
        return new CurationStorageResponseDto(curation.getUser().getId(), curation.getId());
    }

    //팔로우 피드 조회
    public Slice<CurationFollowFeedResponseDto> getFollowFeed(ModugardenUser user, Pageable pageable){
        List<Long> userList = followRepository.listFindByFollowingUser_Id(user.getUserId(),pageable);
        userList.add(user.getUserId());
        Slice<Curation> curationSlice = curationRepository.findCuration(userList,pageable);

        return curationSlice
                .map(u -> new CurationFollowFeedResponseDto(u,likeCurationRepository.findByUserAndCuration(user.getUser(), u).isPresent(),curationStorageRepository.findByUserAndCuration(user.getUser(), u).isPresent()));
    }

    // 해당 유저의 모든 큐레이션 삭제
    @Transactional
    public void deleteAllCurationOfUser(User user){
        List<Curation> allCurationOfUser = curationRepository.findByUser(user);

        for (Curation curation : allCurationOfUser) {
            delete(curation.getId(), user);
        }
        curationRepository.flush();

        likeCurationRepository.deleteByUser(user);
        likeCurationRepository.flush();

        curationStorageRepository.deleteByUser(user);
        curationStorageRepository.flush();
    }

}
